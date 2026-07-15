# Drift Realista RC

Protótipo Android de simulação de drift em rua, com física arcade/realista leve, marcas de pneu, seleção de carros e alternância entre carro real e RC drift.

## Como jogar

- Toque na parte superior da tela para acelerar.
- Toque na parte inferior para frear/ré.
- Toque à esquerda ou à direita para esterçar e iniciar o drift.
- Use **Trocar carro** para alternar entre JDM Turbo, Muscle V8 e RC Drift Rua.
- Use **Modo RC** para reduzir escala, potência e aderência, simulando RC drift na rua.

## Gerar APK

Com Android SDK instalado e `ANDROID_HOME` configurado, execute:

```bash
gradle :app:assembleDebug
```

O APK debug será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

> Observação: o projeto não incorpora nenhum modelo GPT em runtime; a simulação é implementada localmente em Java/Android para funcionar offline.
