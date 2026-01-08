# 🅿️ Parking Auto Renewer - Visão Geral Completa do Projeto

## 📋 Resumo Executivo

**Parking Auto Renewer** é um aplicativo Android que automatiza a renovação de estacionamento em lotes gerenciados pela plataforma **OffStreet** (https://www.offstreet.io/). O app acessa a página de renovação de estacionamento, preenche automaticamente os formulários com dados do usuário e renova o tempo de permanência periodicamente.

---

## 🏗️ Arquitetura Geral

### Componentes Principais

```
┌─────────────────────────────────────────────────────────┐
│              APLICAÇÃO ANDROID                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────┐ │
│  │  MainActivity│    │AutoRenewActivity│  │DebugActivity│
│  │  (Tela       │    │ (Tela Principal)   │(Teste de) │
│  │   Inicial)   │    │                    │Interface) │
│  └──────────────┘    └──────────────┘    └──────────┘ │
│         ↓                   ↓                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │      ParkingAutomationManager                    │  │
│  │  (Lógica de automação com WebView)              │  │
│  └──────────────────────────────────────────────────┘  │
│         ↓                   ↓                           │
│  ┌──────────────┐    ┌──────────────┐                 │
│  │ParkingRenewal│    │ParkingRenewal│                 │
│  │Service (BG)  │    │Worker (BG)   │                 │
│  └──────────────┘    └──────────────┘                 │
│         ↓                   ↓                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │    OffStreet WebView                             │  │
│  │ https://www.offstreet.io/location/LWLN9BUO      │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Estrutura de Arquivos

```
app/src/main/
├── AndroidManifest.xml           # Declaração de componentes e permissões
├── java/com/example/parkingautorenew/
│   ├── MainActivity.kt            # Tela inicial (botões)
│   ├── AutoRenewActivity.kt       # Tela principal de configuração
│   ├── DebugActivity.kt           # Tela para testar coleta de dados
│   ├── ParkingAutomationManager.kt # Orquestrador de automação
│   ├── ParkingRenewalService.kt   # Serviço background
│   ├── ParkingRenewalWorker.kt    # Worker para tarefas periódicas
│   └── ConfirmationDetails.kt     # Data class com detalhes confirmação
└── res/
    ├── layout/
    │   ├── activity_main.xml              # Layout da tela inicial
    │   ├── activity_auto_renew.xml        # Layout da tela principal
    │   └── activity_debug.xml             # Layout da tela debug
    ├── drawable/                         # Ícones e backgrounds
    └── values/                           # Colors, strings
```

---

## 🔑 Componentes Detalhados

### 1️⃣ **MainActivity.kt**
**Função**: Tela inicial do aplicativo

**Elementos**:
- ✨ Ícone do app (logotipo de estacionamento)
- 🔘 Botão "AUTO RENEW" → Abre `AutoRenewActivity`
- 🐞 Botão "Debug Mode" → Abre `DebugActivity`
- 📌 Versão do app (v1.0.2)

**Fluxo**:
```
User launch app
    ↓
MainActivity criada
    ↓
Exibe UI inicial
    ↓
Usuário clica em "AUTO RENEW" ou "Debug Mode"
```

---

### 2️⃣ **AutoRenewActivity.kt** (PRINCIPAL)
**Função**: Tela de configuração e execução de renovação automática

**Campos de Entrada**:
- 📝 Placa do Veículo (ex: ABC1234)
- ⏱️ Tempo de Estacionamento (spinner: 1H, 2H, 3H, etc.)
- 🔄 Frequência de Renovação (spinner: 30min, 1h, 2h, etc.)
- ✉️ Email (opcional - checkbox para habilitar)

**Botões**:
- 🟢 **START** → Inicia renovação automática
- 🔴 **STOP** → Para renovação em execução
- ⬅️ **EXIT** → Retorna para MainActivity

**Indicadores**:
- 📊 Status em tempo real (Aguardando / Executando / Concluído)
- ✅ Contador de Renovações com Sucesso
- ❌ Contador de Falhas
- ⏳ Countdown para próxima renovação
- 📍 Detalhes de Confirmação (quando sucesso)

**Fluxo Técnico**:
```
onCreate()
  ├─ Inflar layout activity_auto_renew.xml
  ├─ Binding de elementos UI (EditText, Spinners, etc)
  ├─ setupAutomationWebView() → Criar WebView oculto
  ├─ setupSpinners() → Inicializar listas de opções
  ├─ setupEmailCheckbox() → Ocultar email por padrão
  ├─ setupButtonListeners()
  │  └─ START button → startAutoRenewal()
  │  └─ STOP button → stopAutoRenewal()
  │  └─ EXIT button → finish()
  ├─ requestNotificationPermission() → Android 13+
  ├─ requestScheduleExactAlarmPermission() → Android 12+
  └─ registerReceiver(renewalBroadcastReceiver) → Escutar updates do Service

User clicks START
  ├─ Validar placa e duração
  ├─ automationManager?.start() → ParkingAutomationManager
  ├─ Esconder botão START, mostrar STOP
  ├─ Mostrar countersLayout
  └─ startCountdownTimer() → Atualizar UI a cada segundo
```

---

### 3️⃣ **DebugActivity.kt**
**Função**: Ferramenta para testar coleta de dados de qualquer página web

**Elementos**:
- 📍 EditText para URL
- 🔘 Botão "GET INFO" → Carrega URL e extrai dados
- 🗑️ Botão "CLEAR" → Limpa WebView
- 📄 TextView com resultado JSON

**Recursos**:
- Injeta JavaScript na página
- Coleta inputs, botões, selects
- Retorna JSON com estrutura HTML
- Permite múltiplas capturas da mesma página (para páginas dinâmicas)

**Útil para**:
- Testar novos sites
- Debug de problemas com coleta de dados
- Validar seletores CSS/JavaScript

---

### 4️⃣ **ParkingAutomationManager.kt** (CORE LOGIC)
**Função**: Orquestrador que controla toda a automação

**URL Alvo**: `https://www.offstreet.io/location/LWLN9BUO` (Alberta)

**Fluxo de 5 Páginas**:

| Página | Descrição | Ações |
|--------|-----------|-------|
| **1** | Página de Boas-vindas | Espera e passa para página 2 |
| **2** | Informações do Veículo | Preenche placa, seleciona región, marca "Lembrar" |
| **3** | Duração de Estacionamento | Seleciona duração (ex: 1 Hour) |
| **4** | Informações de Contato | Preenche dados opcionais |
| **5** | Confirmação de Email | Confirma email e finaliza |

**Fluxo Técnico**:
```
start(plate, duration, shouldSendEmail, email)
  ├─ isExecuting = true
  ├─ setupTimeoutHandler() → 60s timeout de segurança
  ├─ setupWebViewClient()
  │  ├─ WebChromeClient → Capture console.log do JS
  │  └─ WebViewClient
  │     ├─ onPageFinished() → Aguarda 2s e chama captureAndProcessPage()
  │     └─ onReceivedError() → Trata erros de carregamento
  ├─ webView.loadUrl(URL)
  └─ (Loop automático de páginas acontece aqui)

captureAndProcessPage()
  ├─ Injeta JavaScript para detectar página atual
  ├─ Retorna número da página (1-5)
  └─ Chama onPageReady(pageNumber)

onPageReady(pageNumber)
  ├─ Aguarda 1s
  └─ Chama handlePageN()
     ├─ handlePage1() → Aguarda passar para page 2
     ├─ handlePage2() → Preenche placa + região + lembrar
     ├─ handlePage3() → Seleciona duração de estacionamento
     ├─ handlePage4() → Preenche email (se necessário)
     └─ handlePage5() → Confirma e conclui
        └─ Callback onSuccess(confirmationDetails)
```

**JavaScript Utilizado**:
- Detecta página via DOM inspection
- Encontra inputs, selects, buttons por ID ou seletor
- Dispara eventos (change, click) para simular interação
- Retorna dados de confirmação

**Proteções**:
- ✅ Flag `successCalled` → Evita múltiplas conclusões
- ✅ Timeout de 60s → Cancela automação travada
- ✅ Validação de página → Detecta páginas inesperadas
- ✅ Delay entre steps → Aguarda renderização

---

### 5️⃣ **ParkingRenewalService.kt**
**Função**: Serviço background que executa renovação

**Tipo**: `Service` com `foregroundServiceType="dataSync"`

**Responsabilidades**:
- Criar notificação permanente (foreground)
- Executar `ParkingAutomationManager` em background
- Enviar broadcasts de progresso para `AutoRenewActivity`
- Agendamento de alarmes (próxima renovação)

**Broadcasts Enviados**:
```java
Intent("RENEWAL_START")
  .putExtra("plate", plateNumber)

Intent("RENEWAL_UPDATE")
  .putExtra("status", "success" | "error")
  .putExtra("startTime", startTime)
  .putExtra("expiryTime", expiryTime)
  .putExtra("confirmationNumber", confirmationNumber)
  .putExtra("location", location)
```

**Canais de Notificação**:
- 🔵 "parking_auto_renew_channel" → Notificação de progresso
- 🟢 "parking_success_channel" → Sucesso
- 🔴 "parking_error_channel" → Erros

---

### 6️⃣ **ParkingRenewalWorker.kt**
**Função**: Worker para renovações periódicas (via WorkManager)

**Tipo**: `Worker` (executado em thread background)

**Fluxo**:
```
doWork()
  ├─ Verificar se auto-renew está habilitado
  ├─ Verificar se já houve renovação recentemente (< 60s)
  ├─ Recuperar parâmetros do SharedPreferences
  ├─ executeAutomation(plate, duration)
  │  ├─ Criar WebView novo
  │  ├─ Instanciar ParkingAutomationManager
  │  ├─ Aguardar conclusão (Semáforo)
  │  └─ Retornar Result.success() ou Result.retry()
  └─ Salvar tempo da última renovação
```

**Agendamento**:
- Periódico via `WorkManager.enqueueUniquePeriodicWork()`
- Intervalo: Baseado em "Renovar a Cada" (30min, 1h, 2h, etc.)

---

### 7️⃣ **ConfirmationDetails.kt**
**Função**: Data class para armazenar resultado de renovação

```kotlin
data class ConfirmationDetails(
    val startTime: String,        // Ex: "14:30"
    val expiryTime: String,       // Ex: "17:30"
    val plate: String,            // Ex: "ABC1234"
    val location: String,         // Ex: "Downtown Lot"
    val confirmationNumber: String // Ex: "CNF123456"
)
```

---

## 📋 Permissões Necessárias

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- Android 13+ -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" /> <!-- Android 12+ -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" /> <!-- Android 12+ -->
```

---

## 🔧 Configuração Técnica

| Aspecto | Valor |
|--------|-------|
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Kotlin Version** | 1.9.20 |
| **AGP (Gradle)** | 8.2.0 |
| **JDK** | 17 |
| **AndroidX** | Sim |
| **Versão App** | 1.0.2 |

**Dependências**:
- `androidx.core:core-ktx:1.12.0`
- `androidx.appcompat:appcompat:1.6.1`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `androidx.work:work-runtime-ktx:2.8.1` (WorkManager)

---

## 🌐 Fluxo de Usuário Completo

### Cenário: Usuário quer renovar estacionamento a cada 30 minutos

```
1. Abrir App
   └─ MainActivity exibida

2. Clicar "AUTO RENEW"
   └─ AutoRenewActivity carregada

3. Preencher formulário:
   ├─ Placa: "ABC1234"
   ├─ Duração: "1 Hour"
   ├─ Renovar a cada: "30 Minutes"
   ├─ Email: Deixar em branco (opcional)
   └─ Clicar "START"

4. Sistema inicia automação:
   ├─ UI mostra "Status: ⏳ EXECUTANDO RENOVAÇÃO..."
   ├─ ParkingAutomationManager inicia
   ├─ WebView carrega OffStreet
   ├─ JavaScript preenche formulários automaticamente
   ├─ Página 1-5 executadas em sequência
   └─ Renovação concluída!

5. Resultado de sucesso:
   ├─ UI atualiza: "Status: ✅ RENOVAÇÃO CONCLUÍDA COM SUCESSO!"
   ├─ Mostrar detalhes: Placa, Hora de Início, Hora de Expiração
   ├─ Contador de sucessos incrementa
   ├─ Countdown iniciado: "Próxima renovação em 30:00 minutos"
   └─ ParkingRenewalService agenda próxima renovação

6. Trabalho em background:
   ├─ WorkManager agenda tarefas periódicas
   ├─ ParkingRenewalWorker executa a cada 30 min
   ├─ Service envia broadcast com status
   └─ AutoRenewActivity recebe atualizações em tempo real

7. Usuário pode:
   ├─ Ver histórico de renovações (sucessos/falhas)
   ├─ Clicar "STOP" para interromper automação
   └─ Clicar "EXIT" para retornar à tela inicial
```

---

## 🔍 Principais Recursos

| Recurso | Descrição |
|---------|-----------|
| **WebView Automation** | Injeta JS para preencher formulários dinamicamente |
| **Background Service** | Renovação contínua mesmo com app em background |
| **Periodic Tasks** | WorkManager para agendamentos precisos |
| **Real-time Updates** | BroadcastReceiver para UI em tempo real |
| **Error Handling** | Timeouts, retry logic, logging detalhado |
| **Notifications** | Notificações de progresso e sucesso |
| **Logging Extensivo** | Todos os passos registrados em Log.d() |
| **Debug Mode** | Ferramenta para testar coleta de dados |

---

## ⚠️ Limitações Atuais

1. **JavaScript Injection**: Depende da estrutura HTML da OffStreet
   - Mudanças no site quebram a automação
   - Requer ajustes nos seletores CSS/IDs

2. **Dinâmico Content**: Se a página carregar conteúdo dinamicamente após 2s, pode não ser capturado
   - Possível solução: aumentar delay ou adicionar retry

3. **Uma Localização**: URL hardcoded para Alberta
   - Seria necessário UI para selecionar localização

4. **Email**: Suporte opcional não completo
   - Ainda precisa ser implementado no handlePage4/5

---

## 📊 Diagrama de Estados

```
┌─────────────┐
│   IDLE      │ (App parado)
└──────┬──────┘
       │ Usuário clica START
       ↓
┌─────────────────────┐
│  AUTOMATION_RUNNING │ (Processando páginas 1-5)
│                     │
│  Page 1 → Page 2    │
│  Page 2 → Page 3    │
│  Page 3 → Page 4    │
│  Page 4 → Page 5    │
│  Page 5 → SUCCESS   │
│                     │
│  (ou ERROR)         │
└──────┬──────────────┘
       │
       ├─→ SUCCESS
       │   │
       │   ├─ Salvar ConfirmationDetails
       │   ├─ Agendar próxima renovação
       │   └─ Volta a IDLE (aguardando timer)
       │
       └─→ ERROR
           │
           ├─ Log do erro
           ├─ Incrementar contador falhas
           └─ Volta a IDLE
```

---

## 🚀 Como Usar

### Build no macOS:
```bash
cd /Users/humferre/localcode/autorenew
./gradlew :app:assembleDebug --warning-mode all
```

### Build Release:
```bash
./gradlew :app:assembleRelease
```

### Logs em tempo real:
```bash
adb logcat | grep -E "(MainActivity|AutoRenew|ParkingAutomation|ParkingRenewal)"
```

### Debug Mode:
1. Abrir app
2. Clicar no ícone 🐞 "Debug Mode"
3. Inserir URL (ex: https://www.example.com)
4. Clicar "GET INFO"
5. Verificar JSON capturado

---

## 📝 Notas Adicionais

- **Logging**: Implementado `Log.d()` em todos os pontos críticos para fácil debug
- **Error Recovery**: Timeouts e retry logic protegem contra travamentos
- **Broadcast Pattern**: Permite múltiplas telas escutarem mesmos eventos
- **WorkManager**: Mais robusto que AlarmManager para tarefas periódicas
- **SharedPreferences**: Persiste configurações entre execuções

---

**Versão do Documento**: 1.0  
**Data**: Janeiro 8, 2026  
**Autor**: AI Assistant
