# 🏗️ Arquitetura Técnica Detalhada - Parking Auto Renewer

## 🔄 Fluxo de Execução Passo a Passo

### Fase 1: Inicialização (onCreate)

```
USER LAUNCHES APP
    ↓
═══════════════════════════════════════════════════════════
MainActivity::onCreate()
═══════════════════════════════════════════════════════════
├─ super.onCreate(savedInstanceState)
├─ setContentView(R.layout.activity_main) → Infla XML layout
├─ findViewById() → Vincula botão e TextViews
├─ autoRenewBtn.setOnClickListener { → Intent(AutoRenewActivity) }
├─ debugIcon.setOnClickListener { → Intent(DebugActivity) }
└─ versionText.text = "v${BuildConfig.VERSION_NAME}"

USER CLICKS "AUTO RENEW"
    ↓
═══════════════════════════════════════════════════════════
AutoRenewActivity::onCreate()
═══════════════════════════════════════════════════════════
├─ setContentView(R.layout.activity_auto_renew)
├─ Binding de elementos UI:
│  ├─ licensePlateInput (EditText)
│  ├─ parkingDurationSpinner (Spinner: "1H", "2H", "3H"...)
│  ├─ renewalFrequencySpinner (Spinner: "30m", "1h", "2h"...)
│  ├─ emailCheckbox + emailInput
│  ├─ statusText (Status messages)
│  ├─ successCountText / failureCountText
│  ├─ startButton / stopButton / exitButton
│  ├─ countdownText (Timer para próxima renovação)
│  └─ countersLayout (Visibilidade inicial GONE)
│
├─ setupAutomationWebView()
│  └─ Criar WebView oculta para automação
│
├─ setupSpinners()
│  ├─ parkingDurationSpinner.adapter = ArrayAdapter([...])
│  └─ renewalFrequencySpinner.adapter = ArrayAdapter([...])
│
├─ setupEmailCheckbox()
│  └─ Ao marcar, mostrar emailInput
│
├─ setupButtonListeners()
│  ├─ startButton → startAutoRenewal()
│  ├─ stopButton → stopAutoRenewal()
│  └─ exitButton → finish()
│
├─ setupLicensePlateInput()
│  └─ Adicionar TextWatcher para validação
│
├─ createNotificationChannel()
│  └─ Criar canais NotificationManager (Android 8+)
│
├─ requestNotificationPermission() → Android 13+
├─ requestScheduleExactAlarmPermission() → Android 12+
│
├─ registerReceiver(renewalBroadcastReceiver, filter)
│  └─ Escutar "RENEWAL_START" e "RENEWAL_UPDATE" broadcasts
│
└─ UI pronta para entrada do usuário
```

### Fase 2: Início da Automação (startAutoRenewal)

```
USER FILLS FORM AND CLICKS "START"
  • Placa: "ABC1234"
  • Duração: "1 Hour"
  • Frequência: "30 Minutes"
    ↓
═══════════════════════════════════════════════════════════
AutoRenewActivity::startAutoRenewal()
═══════════════════════════════════════════════════════════
├─ Validar licensePlateInput.text não vazio
├─ Recuperar valores:
│  ├─ plateNumber = licensePlateInput.text
│  ├─ duration = parkingDurationSpinner.selectedItem
│  ├─ frequency = renewalFrequencySpinner.selectedItem
│  └─ sendEmail = emailCheckbox.isChecked
│
├─ isRunning = true
├─ startButton.visibility = GONE
├─ stopButton.visibility = VISIBLE
├─ countersLayout.visibility = VISIBLE
│
├─ statusText.text = "Status: ⏳ EXECUTANDO RENOVAÇÃO..."
│
├─ Iniciar automação:
│  └─ automationManager?.start(
│      plate = plateNumber,
│      duration = duration,
│      shouldSendEmail = sendEmail,
│      email = email
│    )
│
├─ startCountdownTimer()
│  └─ Atualizar UI a cada 1 segundo
│
└─ Iniciar serviço em background:
    └─ startForegroundService(Intent(ParkingRenewalService))
```

### Fase 3: ParkingAutomationManager Executa

```
ParkingAutomationManager::start() CHAMADO
    ↓
═══════════════════════════════════════════════════════════
ParkingAutomationManager::start(plate, duration, ...)
═══════════════════════════════════════════════════════════
├─ if (isExecuting) return → Evitar múltiplas execuções
├─ isExecuting = true
├─ successCalled = false → Reset flag
├─ plateNumber = plate
├─ parkingDuration = duration
├─ sendEmail = shouldSendEmail
├─ userEmail = email
├─ currentPage = 1
│
├─ setupTimeoutHandler()
│  └─ mainHandler.postDelayed(timeoutRunnable, 60000)
│     └─ Se não completar em 60s, chamar onError()
│
├─ setupWebViewClient()
│  ├─ webChromeClient → Capture console.log JS
│  └─ webViewClient
│     ├─ onPageFinished(view, url)
│     │  ├─ if (url = "about:blank" || successCalled) return
│     │  ├─ Handler().postDelayed({
│     │  │   captureAndProcessPage()
│     │  │ }, 2000ms)
│     │  └─ Aguarda página renderizar completamente
│     │
│     └─ onReceivedError(view, request, error)
│        ├─ isExecuting = false
│        └─ onError("Erro ao carregar: ${error}")
│
├─ webView.loadUrl("https://www.offstreet.io/location/LWLN9BUO")
│
└─ (Agora aguardando onPageFinished())
```

### Fase 4: Detecção e Processamento de Páginas

```
onPageFinished() CHAMADO (após 2 segundos)
    ↓
═══════════════════════════════════════════════════════════
ParkingAutomationManager::captureAndProcessPage()
═══════════════════════════════════════════════════════════
├─ if (successCalled || !isExecuting) return
│
├─ Injetar JavaScript para detectar página:
│  │
│  └─ Script JavaScript:
│     ├─ const hasPlateInput = document.getElementById('plate')
│     ├─ const hasDurationButtons = [botões com "Hour Parking"]
│     ├─ const hasRegisterButton = [botão "REGISTER"]
│     ├─ const hasEmailInput = document.getElementById('email')
│     │
│     ├─ Lógica de detecção:
│     │  ├─ Se hasEmailInput → detectedPage = 5
│     │  ├─ Senão se hasRegisterButton → detectedPage = 4
│     │  ├─ Senão se hasDurationButtons → detectedPage = 3
│     │  ├─ Senão se hasPlateInput → detectedPage = 2
│     │  └─ Senão → detectedPage = 1
│     │
│     └─ Retornar { page: detectedPage, title: ..., url: ... }
│
├─ webView.evaluateJavascript(script) { result →
│  ├─ Parse JSON result
│  ├─ Extrair "page": N
│  ├─ currentPage = N
│  └─ Chamar onPageReady(currentPage)
│  }
│
└─ (Prosseguir para handler de página específica)
```

### Fase 5: Handlers de Página (Page 1-5)

#### **Page 1: Boas-vindas**
```
handlePage1()
  └─ Handler().postDelayed({
     currentPage = 2
     captureAndProcessPage() ← Carregar próxima página
   }, LOAD_DELAY)
```

#### **Page 2: Informações do Veículo**
```
handlePage2()
  │
  ├─ Injetar JavaScript:
  │  ├─ plateInput.value = "ABC1234"
  │  ├─ Disparar evento 'change' em plateInput
  │  ├─ regionSelect.value = "Alberta"
  │  ├─ Disparar evento 'change' em regionSelect
  │  ├─ rememberCheckbox.checked = true
  │  ├─ Disparar evento 'change' em checkbox
  │  └─ Encontrar botão "NEXT" e click()
  │
  ├─ webView.evaluateJavascript(script) { result →
  │  └─ Handler().postDelayed({
  │     currentPage = 3
  │     captureAndProcessPage() ← Carregar próxima página
  │   }, LOAD_DELAY)
  │  }
  │
  └─ TRANSIÇÃO: Page 2 → Page 3
```

#### **Page 3: Duração de Estacionamento**
```
handlePage3()
  │
  ├─ Injetar JavaScript:
  │  ├─ Encontrar todos os botões com "Hour Parking"
  │  ├─ Encontrar botão com texto = "parkingDuration" (ex: "1 Hour Parking")
  │  ├─ Clicar no botão correspondente
  │  └─ Handler().postDelayed({
  │     └─ Próximo botão (ex: "Continue" ou "Next")
  │     }
  │
  └─ Handler().postDelayed({
     currentPage = 4
     captureAndProcessPage()
   }, LOAD_DELAY)
```

#### **Page 4: Informações de Contato**
```
handlePage4()
  │
  ├─ if (sendEmail) {
  │  ├─ emailInput.value = userEmail
  │  ├─ Disparar evento 'change'
  │  └─ Encontrar botão "NEXT" e click()
  │  }
  │
  └─ Handler().postDelayed({
     currentPage = 5
     captureAndProcessPage()
   }, LOAD_DELAY)
```

#### **Page 5: Confirmação e Conclusão**
```
handlePage5()
  │
  ├─ Injetar JavaScript:
  │  ├─ startTime = document.getElementById('startTime').innerText
  │  ├─ expiryTime = document.getElementById('expiryTime').innerText
  │  ├─ confirmationNumber = document.getElementById('confirmation').innerText
  │  ├─ location = document.getElementById('location').innerText
  │  │
  │  └─ Encontrar botão "CONFIRM" ou similar e click()
  │
  └─ WebView detectará nova página:
     ├─ onPageFinished() chamado
     ├─ Se sucesso, página mudará para confirmação
     ├─ JavaScript detectará mudança
     └─ Callback onSuccess(confirmationDetails) executado
```

### Fase 6: Sucesso e Conclusão

```
onSuccess(confirmationDetails) CHAMADO
    ↓
═══════════════════════════════════════════════════════════
ParkingAutomationManager::onSuccess()
═══════════════════════════════════════════════════════════
├─ if (successCalled) return → Evitar duplicata
├─ successCalled = true
├─ isExecuting = false
├─ cancelTimeoutHandler()
├─ Callback: onSuccess(confirmationDetails) executado
│  └─ Callback definido por quem criou o manager

USER VÊ UI:
  "Status: ✅ RENOVAÇÃO CONCLUÍDA COM SUCESSO!"
  
AutoRenewActivity::renewalBroadcastReceiver RECEBE:
  Intent("RENEWAL_UPDATE")
    .putExtra("status", "success")
    .putExtra("startTime", "14:30")
    .putExtra("expiryTime", "17:30")
    .putExtra("plate", "ABC1234")
    .putExtra("location", "Downtown")
    .putExtra("confirmationNumber", "CNF123456")
    ↓
autoRenewActivity::onReceive()
  ├─ Criar ConfirmationDetails
  ├─ lastConfirmationDetails = confirmationDetails
  ├─ incrementSuccessCount()
  ├─ statusText.text = "✅ RENOVAÇÃO CONCLUÍDA COM SUCESSO!"
  │
  ├─ Handler().postDelayed({
  │  ├─ updateStatusWithConfirmation(confirmationDetails)
  │  │  └─ Mostrar detalhes: placa, horários, confirmação
  │  └─ startCountdownTimer()
  │     └─ Contar regressiva até próxima renovação
  │  }, 1500ms)
  │
  └─ ParkingRenewalService agenda próxima renovação:
     ├─ WorkManager.enqueueUniquePeriodicWork(
     │  tag = "parking_auto_renew",
     │  existingPeriodicPolicy = KEEP,
     │  request = PeriodicWorkRequestBuilder<ParkingRenewalWorker>(
     │    interval = frequency (30m, 1h, etc)
     │  )
     │  )
     └─ Próxima execução em frequency minutos
```

---

## 🌐 Integração WebView e JavaScript

### Interface JavaScript ↔ Kotlin

```kotlin
// Em ParkingAutomationManager
webView.addJavascriptInterface(PageBridge(), "Android")

inner class PageBridge {
    @JavascriptInterface
    fun onStepComplete(step: String) {
        // Chamado por: Android.onStepComplete("page2_filled")
        Log.d(TAG, "Step completed: $step")
    }
    
    @JavascriptInterface
    fun onError(error: String) {
        // Chamado por: Android.onError("Erro page 2: ...")
        Log.e(TAG, "Error from JS: $error")
        onError(error)
    }
    
    @JavascriptInterface
    fun onPageInfo(json: String) {
        // Chamado por: Android.onPageInfo(JSON.stringify({...}))
        Log.d(TAG, "Page info received: $json")
    }
}
```

### Exemplo de JavaScript Injetado

```javascript
// Script para Page 2 (Vehicle Info)
(function(){
  try {
    // Preencher entrada de placa
    const plateInput = document.getElementById('plate');
    if (plateInput) {
      plateInput.value = 'ABC1234';
      plateInput.dispatchEvent(new Event('input', { bubbles: true }));
      plateInput.dispatchEvent(new Event('change', { bubbles: true }));
    }
    
    // Selecionar estado
    const regionSelect = document.getElementById('region');
    if (regionSelect) {
      regionSelect.value = 'Alberta';
      regionSelect.dispatchEvent(new Event('change', { bubbles: true }));
    }
    
    // Marcar checkbox
    const rememberCheckbox = document.getElementById('rememberPlate');
    if (rememberCheckbox) {
      rememberCheckbox.checked = true;
      rememberCheckbox.dispatchEvent(new Event('change', { bubbles: true }));
    }
    
    // Clicar botão
    const nextButton = Array.from(document.querySelectorAll('button'))
      .find(b => b.textContent.toLowerCase() === 'next');
    if (nextButton) {
      nextButton.click();
    }
    
    // Notificar sucesso
    if (typeof Android !== 'undefined' && Android.onStepComplete) {
      Android.onStepComplete('page2_filled');
    }
  } catch(e) {
    // Notificar erro
    if (typeof Android !== 'undefined' && Android.onError) {
      Android.onError('Erro page 2: ' + e.message);
    }
  }
})();
```

---

## 📱 Background Execution (Serviço + Worker)

### Serviço Foreground (ParkingRenewalService)

```
Sistema executa:
  startForegroundService(Intent(ParkingRenewalService))
    ↓
ParkingRenewalService::onCreate()
  ├─ createNotificationChannel()
  │  ├─ CHANNEL_ID = "parking_auto_renew_channel"
  │  ├─ SUCCESS_CHANNEL_ID = "parking_success_channel"
  │  └─ ERROR_CHANNEL_ID = "parking_error_channel"
  └─ NÃO criar WebView aqui (criar novo para cada renovação)

ParkingRenewalService::onStartCommand()
  ├─ Criar Notification permanente
  ├─ startForeground(NOTIFICATION_ID, notification)
  ├─ Recuperar parâmetros do Intent
  ├─ Iniciar automação:
  │  └─ automationManager.start(plate, duration, ...)
  │
  ├─ Escutar callback de sucesso/erro:
  │  ├─ onSuccess() → sendBroadcast("RENEWAL_UPDATE", status="success")
  │  └─ onError() → sendBroadcast("RENEWAL_UPDATE", status="error")
  │
  └─ return START_REDELIVER_INTENT ou START_NOT_STICKY
```

### WorkManager Periodic Task (ParkingRenewalWorker)

```
WorkManager::enqueueUniquePeriodicWork()
  (Agendado com intervalo = frequência de renovação)
    ↓
ParkingRenewalWorker::doWork() EXECUTADO
  │
  ├─ Recuperar parâmetros de SharedPreferences
  ├─ Verificar se auto-renew está habilitado
  ├─ Verificar se houve renovação recente (< 60s)
  ├─ Criar WebView novo
  ├─ executeAutomation(plate, duration)
  │  ├─ Criar ParkingAutomationManager
  │  ├─ Aguardar conclusão via Semáforo
  │  └─ Retornar true se sucesso
  │
  ├─ Atualizar SharedPreferences com tempo da renovação
  │
  └─ return Result.success() ou Result.retry()
     ├─ Result.success() → Próxima execução agendada automaticamente
     └─ Result.retry() → Retry automático em alguns minutos
```

---

## 💾 Persistência de Dados (SharedPreferences)

```
SharedPreferences("parking_prefs", Context.MODE_PRIVATE)
  │
  ├─ "auto_renew_enabled" → boolean (start/stop)
  ├─ "license_plate" → String (placa salva)
  ├─ "parking_duration" → String (duração salva)
  ├─ "renewal_frequency" → String (frequência salva)
  ├─ "last_renewal_time" → Long (timestamp ms)
  ├─ "success_count" → Int (renovações bem-sucedidas)
  ├─ "failure_count" → Int (renovações falhadas)
  │
  └─ Usado por:
     ├─ AutoRenewActivity (carregar/salvar user input)
     ├─ ParkingRenewalService (recuperar params)
     └─ ParkingRenewalWorker (verificar flags)
```

---

## 🔔 Broadcast Communication Pattern

```
┌────────────────────────────────┐
│   ParkingRenewalService        │
│  (Running in background)       │
├────────────────────────────────┤
│                                │
│  onSuccess(details) →          │
│    sendBroadcast(             │
│      Intent("RENEWAL_UPDATE")  │
│      .putExtra("status", "ok")  │
│    )                           │
│                                │
│  onError(error) →              │
│    sendBroadcast(             │
│      Intent("RENEWAL_UPDATE")  │
│      .putExtra("status", "err") │
│    )                           │
└────────────────────────────────┘
          ↓ (Intent)
┌────────────────────────────────┐
│  BroadcastReceiver            │
│  (AutoRenewActivity)          │
├────────────────────────────────┤
│                                │
│  onReceive() {                │
│    val status = intent.        │
│      getStringExtra("status")  │
│    if (status == "success") {  │
│      updateUI()                │
│      incrementCounter()        │
│    }                           │
│  }                             │
│                                │
└────────────────────────────────┘
```

---

## 🛡️ Error Handling & Timeout Protection

```
Automação iniciada
  │
  ├─ setupTimeoutHandler()
  │  └─ Agendar: mainHandler.postDelayed(timeout, 60000ms)
  │
  └─ Executar lógica de automação
     │
     ├─ Se completar antes de 60s:
     │  ├─ cancelTimeoutHandler()
     │  └─ onSuccess() chamado
     │
     └─ Se NÃO completar em 60s:
        ├─ timeoutRunnable executado
        ├─ isExecuting = false
        └─ onError("Timeout na automação (60 segundos)")
```

---

## 📊 Contadores e Status

```
AutoRenewActivity
  │
  ├─ successCountText: Int
  │  └─ Incrementado quando onSuccess()
  │
  ├─ failureCountText: Int
  │  └─ Incrementado quando onError()
  │
  ├─ countdownText: String
  │  └─ Countdown até próxima renovação
  │     Formato: "MM:SS" (ex: "30:00")
  │
  ├─ statusText: String
  │  └─ Mensagens de status:
  │     ├─ "⏳ EXECUTANDO RENOVAÇÃO..."
  │     ├─ "✅ RENOVAÇÃO CONCLUÍDA COM SUCESSO!"
  │     └─ "❌ Erro na renovação: [mensagem]"
  │
  └─ totalTimeText: String
     └─ Tempo total desde início de execução
```

---

## 🔐 Proteções Contra Múltiplas Execuções

```
Mecanismo 1: Flag isExecuting
  ├─ start() { if (isExecuting) return }
  └─ Evita iniciar automação 2x simultaneamente

Mecanismo 2: Flag successCalled
  ├─ onSuccess() { if (successCalled) return }
  └─ Evita chamar callbacks múltiplas vezes

Mecanismo 3: Verificação onPageFinished
  ├─ if (url = "about:blank" || successCalled) return
  └─ Ignora carregamentos de páginas em branco/após sucesso

Mecanismo 4: ParkingRenewalWorker - Validação
  ├─ if (!isEnabled) return Result.success()
  ├─ if (timeSinceLastRenewal < 60000) return Result.success()
  └─ Evita renovações muito frequentes
```

---

## 📲 Telas Visuais

### MainActivity
- Ícone de estacionamento
- Botão "AUTO RENEW" (destacado)
- Botão "Debug Mode" (pequeno)
- Versão do app

### AutoRenewActivity
```
┌─────────────────────────────┐
│  Auto Parking Renewer       │
├─────────────────────────────┤
│                             │
│ Placa do Veículo:           │
│ [ABC1234________________]    │
│                             │
│ Tempo de Estacionamento:    │
│ [1 Hour            ▼]       │
│                             │
│ Renovar a Cada:             │
│ [30 Minutes        ▼]       │
│                             │
│ ☐ Enviar Email              │
│                             │
│ Status: ⏳ EXECUTANDO...     │
│                             │
│ ✅ Sucessos: 5              │
│ ❌ Falhas: 0                │
│ ⏳ Próxima: 30:00           │
│                             │
│ [START] [STOP] [EXIT]       │
│                             │
└─────────────────────────────┘
```

### DebugActivity
```
┌─────────────────────────────┐
│  Debug Mode                 │
├─────────────────────────────┤
│ URL:                        │
│ [https://...____________]   │
│                             │
│ [GET INFO] [CLEAR]          │
│                             │
│ JSON Result:                │
│                             │
│ {                           │
│   "page": 3,                │
│   "title": "...",           │
│   "inputs": [...]           │
│ }                           │
│                             │
└─────────────────────────────┘
```

---

## 🎯 Padrões de Design Utilizados

| Padrão | Uso |
|--------|-----|
| **Callback/Listener** | onSuccess/onError em ParkingAutomationManager |
| **Broadcast Pattern** | Comunicação Service → Activity |
| **Singleton-like WebView** | Uma instância reutilizada durante automação |
| **State Machine** | Estados de página (1-5) |
| **Timeout/Watchdog** | Proteção contra travamentos |
| **Retry Logic** | WorkManager Result.retry() |
| **Foreground Service** | Manter app "vivo" durante background task |
| **Dependency Injection** | WebView passado ao ParkingAutomationManager |

---

**Versão do Documento**: 1.0  
**Data**: Janeiro 8, 2026
