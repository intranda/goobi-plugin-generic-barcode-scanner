package de.intranda.goobi.plugins.barcode;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.intranda.goobi.plugins.barcode.config.BarcodeFormat;
import de.intranda.goobi.plugins.barcode.config.BarcodeScannerPluginConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.lang3.StringUtils;
import org.goobi.production.plugin.DockAnchor;
import org.goobi.production.plugin.interfaces.AbstractGenericPlugin;

import jakarta.annotation.ManagedBean;
import jakarta.enterprise.context.RequestScoped;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Data
@ManagedBean
@RequestScoped
@PluginImplementation
public class BarcodeScannerPlugin extends AbstractGenericPlugin {
    @Override
    public String getId() {
        return getTitle().replaceAll("\\s+", "");
    }

    @Override
    public String getIcon() {
        return "fa-barcode";
    }

    @Override
    public boolean isDockable(DockAnchor anchor) {
        try {
            return config().getDocking().contains(anchor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute() throws Exception {
        success("Barcode plugin \"" + getTitle() + "\" executed!");
    }

    @Override
    public String getTitle() {
        return "Barcode Scanner";
    }

    @Override
    public String getModalPath() {
        return "../includes/barcodePlugin/barcodeModal.xhtml";
    }

    private String code;
    private BarcodeScannerPluginConfiguration config;
    private List<BarcodeFormat> activeFormats = Collections.emptyList();
    private boolean showConfig;
    private BarcodeFormat selectedConfigFormat;
    private List<BarcodeField> barcodeFields;
    private String barcode;

    @Data
    public class BarcodeField {
        private String label;
        private String value;

        public void setValue(String value) {
            this.value = value;
            updateBarcode();
        }
    }

    public void setSelectedConfigFormat(BarcodeFormat selectedConfigFormat) {
        this.selectedConfigFormat = selectedConfigFormat;
        updateBarcodeFields();
        updateBarcode();
    }

    private void updateBarcodeFields() {
        if (this.selectedConfigFormat == null) {
            return;
        }
        String p = this.selectedConfigFormat.getPattern();
        int numberOfFields = StringUtils.countMatches(p, "(");
        barcodeFields = new ArrayList<>(numberOfFields);
        List<String> sampleValues = this.selectedConfigFormat.getSampleValues();
        for (int i = 0; i < numberOfFields; i++) {
            BarcodeField field = new BarcodeField();
            field.setLabel("Value " + (i+1));
            field.setValue(sampleValues.get(i));
            barcodeFields.add(field);
        }
    }

    private void updateBarcode() {
        if (this.selectedConfigFormat == null) {
            return;
        }
        this.barcode = this.selectedConfigFormat.getPattern();
        for (int i = 0; i < this.barcodeFields.size(); i++) {
            this.barcode = this.barcode.replaceFirst("\\(.*?\\)", this.barcodeFields.get(i).getValue());
        }
    }

    @Override
    public void initialize() throws Exception {
        config(); // Load config
    }

    private BarcodeScannerPluginConfiguration config() throws IOException {
        if (this.config == null) {
            XmlMapper mapper = new XmlMapper();
            this.config = mapper.readValue(new File("/opt/digiverso/goobi/config/plugin_intranda_generic_barcodeScanner.xml"), BarcodeScannerPluginConfiguration.class);
        }
        return this.config;
    }

    public void scan() {
        log.debug("Processing barcode \"{}\"", code);
        String barcode = code;
        this.code = "";

        try {
            List<BarcodeFormat> applicable = findMatchingBarcodeFormats(barcode);

            if (!applicable.isEmpty()) {
                applicable.forEach(bf -> bf.activate(barcode));
                activeFormats = applicable;
            } else {
                if (!activeFormats.isEmpty()) {
                    activeFormats.forEach(bf -> bf.execute(barcode));
                } else {
                    warn("No active barcode actions found!");
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            error("An error occurred while scanning barcode \"" + barcode + "\"!");
        }
    }

    private List<BarcodeFormat> findMatchingBarcodeFormats(String code) {
        return this.config.getBarcode().stream()
                .filter(bf -> bf.patternMatches(code))
                .toList();
    }
}