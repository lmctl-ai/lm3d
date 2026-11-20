import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const compilerModule = process.env.LMCODE_COMPILER_MODULE;
if (!compilerModule) throw new Error('Set LMCODE_COMPILER_MODULE to the @lmctl-ai/lmctl dist/dsl.js path');
const sourcePath = path.join(root, 'src/main/lmcode/scenes.lms');
const schemaPath = path.join(root, 'src/main/lmcode/scenes.schema.json');
const source = readFileSync(sourcePath, 'utf8');
const schemaBytes = readFileSync(schemaPath);
const { LMScript, CompiledDsl } = await import(pathToFileURL(compilerModule).href);
const result = new LMScript().run(source, { baseDir: path.dirname(sourcePath) });
if (!result.success) throw new Error(JSON.stringify(result.diagnostics));
const bytecode = CompiledDsl.bytesOf(result.value);

const java = `package com.lmctl.lm3d.demo;

final class LmcodeScenesProgram {
    static final String BYTECODE = "${Buffer.from(bytecode).toString('base64')}";
    static final String SCHEMA = ${JSON.stringify(schemaBytes.toString('utf8'))};

    private LmcodeScenesProgram() {
    }
}
`;
const output = path.join(root, 'target/generated-sources/lmcode/com/lmctl/lm3d/demo/LmcodeScenesProgram.java');
mkdirSync(path.dirname(output), { recursive: true });
writeFileSync(output, java);
