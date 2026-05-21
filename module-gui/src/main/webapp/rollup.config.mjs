import { nodeResolve } from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import terser from '@rollup/plugin-terser';

export default {
    input: 'js/barcode.js',
    output: {
        file: 'resources/plugins/js/jsbarcode.min.js',
        format: 'iife',
    },
    plugins: [
        nodeResolve({ browser: true }),
        commonjs(),
        terser(),
    ],
};
