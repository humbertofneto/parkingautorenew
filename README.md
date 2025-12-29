# Parking Info (Android, Kotlin)

App mínimo que abre a página `https://www.offstreet.io/location/LWLN9BUO` e, ao tocar o botão "Get Info", coleta campos visíveis (inputs, botões, selects) e mostra o JSON no texto da tela.

**O que está implementado**
- **Tela:** botão `Get Info` e área de texto para resultados.
- **Funcionalidade:** carrega a página em WebView, injeta JavaScript, coleta e exibe JSON.
- **Permissões:** apenas `INTERNET` no Manifest.

**Arquivos principais**
- [app/src/main/java/com/example/parkingautorenew/MainActivity.kt](app/src/main/java/com/example/parkingautorenew/MainActivity.kt): lógica do botão, WebView e extração via JS.
- [app/src/main/res/layout/activity_main.xml](app/src/main/res/layout/activity_main.xml): layout com botão e texto.
- [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml): app mínimo com permissão de internet.

**Versões (AGP/Kotlin) via Version Catalog**
- Controladas em [gradle/libs.versions.toml](gradle/libs.versions.toml).
- Plugins usam aliases em [build.gradle.kts](build.gradle.kts), facilitando ajustes entre máquinas.

## Build no Windows (Android Studio)
- **JDK:** selecione JDK 17 em Gradle Settings.
- **Limpeza opcional:**
```powershell
cd C:\Users\Admin\Desktop\LocalCode\ParkingAutoRenew
Remove-Item -Recurse -Force .gradle, build, app\build
```
- **Sync e build:**
   - Android Studio: "Sync Project with Gradle Files" e "Make Project".
   - Com wrapper (se presente):
```powershell
cd C:\Users\Admin\Desktop\LocalCode\ParkingAutoRenew
.\u0067radlew :app:assembleDebug --warning-mode all
```
- **Sem wrapper:** instale Gradle e gere o wrapper (recomendado):
```powershell
winget install Gradle.Gradle
cd C:\Users\Admin\Desktop\LocalCode\ParkingAutoRenew
gradle wrapper --gradle-version 8.7 --distribution-type bin
.\u0067radlew :app:assembleDebug --warning-mode all
```

## Build no macOS
```bash
cd /Users/humferre/localcode/ParkingAutoRenew
./gradlew :app:assembleDebug --warning-mode all
```

## Executando
- Instale e abra o app, toque "Get Info" e aguarde.
- O JSON com título/url/inputs/botões/selects será exibido.

## Problemas comuns e solução
- **Erro envolvendo `debugRuntimeClasspathCopy`:** garanta que o `app/build.gradle.kts` atual não tem blocos `configurations {...}` ou `afterEvaluate {...}` — já removidos neste projeto. Limpe `.gradle` e `build`, reinicie o Android Studio e sincronize.
- **Incompatibilidade de versões:** use Gradle 8.7 e JDK 17; AGP/Kotlin podem ser ajustados em [gradle/libs.versions.toml](gradle/libs.versions.toml).
- **Conteúdo dinâmico não coletado:** podemos adicionar delay/retry na execução do JavaScript. Abra uma issue ou peça ajuste.
