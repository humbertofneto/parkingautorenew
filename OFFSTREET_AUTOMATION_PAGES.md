# 🌐 OffStreet Automation Flow - Guia de Páginas e Scripts

## 📍 URL Alvo
```
https://www.offstreet.io/location/LWLN9BUO
Localização: Alberta, Canada
```

---

## 🔢 Fluxo de 5 Páginas

### 📄 **PÁGINA 1: Welcome / Landing Page**

**Propósito**: Apresentação e boas-vindas

**Elementos Esperados**:
- ✨ Título/Logo do site
- 📝 Descrição da localização
- 🔘 Botão "Get Parking" ou "Start" (opcional)

**Estratégia de Automação**:
```javascript
// handlePage1() em ParkingAutomationManager
// Apenas aguarda renderização e passa para página 2
// Ou detecta clique em botão "Start" se necessário

Handler().postDelayed({
  currentPage = 2
  captureAndProcessPage()
}, LOAD_DELAY)
```

**Detecção em captureAndProcessPage()**:
```javascript
const hasPlateInput = document.getElementById('plate') !== null;
const hasDurationButtons = Array.from(document.querySelectorAll('button'))
  .some(b => b.textContent.includes('Hour Parking'));

// Se ambos são falsos → Page 1
if (!hasPlateInput && !hasDurationButtons) {
  detectedPage = 1;
}
```

**Possíveis Variações**:
- ✓ Redirecionamento automático para page 2 (sem ação)
- ✓ Necessário clicar em botão "Start" (JavaScript injeta clique)

---

### 🚗 **PÁGINA 2: Vehicle Information**

**Propósito**: Coletar informações do veículo (placa) e região

**Elementos Esperados**:
```html
<input id="plate" type="text" placeholder="Placa do Veículo" />
<select id="region">
  <option>Alberta</option>
  <option>Ontario</option>
  <option>Quebec</option>
  ...
</select>
<input id="rememberPlate" type="checkbox" label="Lembrar Placa" />
<button>Next</button>
```

**Script de Automação (handlePage2)**:
```kotlin
val licensePlate = "ABC1234"  // Fornecido pelo usuário

val script = """
  (function(){
    try {
      // 1. Preencher placa
      const plateInput = document.getElementById('plate');
      if (plateInput) {
        plateInput.value = '$licensePlate';
        plateInput.dispatchEvent(new Event('input', { bubbles: true }));
        plateInput.dispatchEvent(new Event('change', { bubbles: true }));
      }
      
      // 2. Selecionar região
      const regionSelect = document.getElementById('region');
      if (regionSelect) {
        regionSelect.value = 'Alberta';  // HARDCODED (TODO: tornar dinâmico)
        regionSelect.dispatchEvent(new Event('change', { bubbles: true }));
      }
      
      // 3. Marcar "Remember Plate"
      const rememberCheckbox = document.getElementById('rememberPlate');
      if (rememberCheckbox) {
        rememberCheckbox.checked = true;
        rememberCheckbox.dispatchEvent(new Event('change', { bubbles: true }));
      }
      
      // 4. Clicar botão "Next"
      const nextButton = Array.from(document.querySelectorAll('button'))
        .find(b => b.textContent.toLowerCase() === 'next');
      if (nextButton) {
        nextButton.click();
      }
      
      // 5. Notificar sucesso
      if (typeof Android !== 'undefined' && Android.onStepComplete) {
        Android.onStepComplete('page2_filled');
      }
    } catch(e) {
      if (typeof Android !== 'undefined' && Android.onError) {
        Android.onError('Erro page 2: ' + e.message);
      }
    }
  })();
"""
```

**Detecção em captureAndProcessPage()**:
```javascript
const hasPlateInput = document.getElementById('plate') !== null;
const hasDurationButtons = Array.from(document.querySelectorAll('button'))
  .some(b => b.textContent.includes('Hour Parking'));

// Se tem plate input E NÃO tem duration buttons → Page 2
if (hasPlateInput && !hasDurationButtons) {
  detectedPage = 2;
}
```

**Timeline de Eventos**:
```
1. User/App preenche placa
2. Select region muda (trigger validation)
3. Checkbox marcado (opcional)
4. Botão "Next" clicado
5. onPageFinished() chamado para nova página
6. Handler aguarda 2s → captureAndProcessPage() chamado
```

**Possíveis Problemas**:
- ⚠️ IDs dos elementos mudaram (site atualizado)
  - Solução: Usar seletores CSS alternativos (label + input)
- ⚠️ Select region tem valores diferentes (ex: "AB" vs "Alberta")
  - Solução: Ler opções disponíveis e matching parcial
- ⚠️ Placa com caracteres especiais
  - Solução: Validar e sanitizar input

---

### ⏱️ **PÁGINA 3: Parking Duration**

**Propósito**: Selecionar tempo de estacionamento (1H, 2H, 3H, etc.)

**Elementos Esperados**:
```html
<div class="parking-options">
  <button class="duration-btn">1 Hour Parking</button>
  <button class="duration-btn">2 Hour Parking</button>
  <button class="duration-btn">3 Hour Parking</button>
  <button class="duration-btn">4 Hour Parking</button>
  ...
</div>
<button id="continueBtn">Continue</button>
```

**Script de Automação (handlePage3)**:
```kotlin
val parkingDuration = "1 Hour"  // Ex: "1 Hour", "2 Hour", etc.

val script = """
  (function(){
    try {
      // 1. Encontrar botão de duração correspondente
      const durationButton = Array.from(document.querySelectorAll('button'))
        .find(b => b.textContent.includes('$parkingDuration + Hour Parking'));
      
      if (durationButton) {
        // Pode ser necessário scroll para botão estar visível
        durationButton.scrollIntoView({ behavior: 'smooth' });
        
        // Aguardar um pouco e clicar
        setTimeout(() => {
          durationButton.click();
          
          // Disparar evento para notificar seleção
          durationButton.dispatchEvent(new Event('click', { bubbles: true }));
        }, 300);
      } else {
        throw new Error('Duration button not found: $parkingDuration Hour Parking');
      }
      
      // 2. Encontrar e clicar botão "Continue"
      setTimeout(() => {
        const continueBtn = Array.from(document.querySelectorAll('button'))
          .find(b => b.textContent.toLowerCase().includes('continue'));
        
        if (continueBtn) {
          continueBtn.click();
        }
      }, 500);
      
    } catch(e) {
      if (typeof Android !== 'undefined' && Android.onError) {
        Android.onError('Erro page 3: ' + e.message);
      }
    }
  })();
"""
```

**Detecção em captureAndProcessPage()**:
```javascript
const hasDurationButtons = Array.from(document.querySelectorAll('button'))
  .some(b => b.textContent.includes('Hour Parking'));
const hasRegisterButton = Array.from(document.querySelectorAll('button'))
  .some(b => b.textContent.toUpperCase() === 'REGISTER');

// Se tem duration buttons E NÃO tem register button → Page 3
if (hasDurationButtons && !hasRegisterButton) {
  detectedPage = 3;
}
```

**Timeline**:
```
1. Botões de duração são renderizados
2. JavaScript encontra botão correspondente
3. Scroll para tornar visível
4. Click no botão
5. Estado muda (ex: destaque ou disable outros)
6. Continue/Next botão clicado
7. onPageFinished() chamado
8. Handler aguarda 2s → Page 4
```

**Casos de Teste**:
- "1 Hour" → "1 Hour Parking"
- "2 Hour" → "2 Hour Parking"
- "All Day" → "All Day Parking" (se disponível)

---

### 📧 **PÁGINA 4: Contact Information (OPCIONAL)**

**Propósito**: Coletar informações de contato (email, telefone)

**Elementos Esperados**:
```html
<input id="email" type="email" placeholder="seu@email.com" />
<input id="phone" type="tel" placeholder="(123) 456-7890" />
<button>Next</button>
```

**Script de Automação (handlePage4)**:
```kotlin
val userEmail = "user@example.com"  // Do checkbox do user
val sendEmail = true  // Flag de checkbox

val script = """
  (function(){
    try {
      if ($sendEmail) {
        // 1. Preencher email
        const emailInput = document.getElementById('email');
        if (emailInput) {
          emailInput.value = '$userEmail';
          emailInput.dispatchEvent(new Event('input', { bubbles: true }));
          emailInput.dispatchEvent(new Event('change', { bubbles: true }));
        }
        
        // 2. (Opcional) Preencher telefone se necessário
        const phoneInput = document.getElementById('phone');
        if (phoneInput) {
          // phoneInput.value = '...';
        }
      }
      
      // 3. Clicar Next
      const nextButton = Array.from(document.querySelectorAll('button'))
        .find(b => b.textContent.toLowerCase() === 'next');
      if (nextButton) {
        nextButton.click();
      }
      
    } catch(e) {
      if (typeof Android !== 'undefined' && Android.onError) {
        Android.onError('Erro page 4: ' + e.message);
      }
    }
  })();
"""
```

**Detecção**:
```javascript
const hasEmailInput = document.getElementById('email') !== null;
const hasRegisterButton = Array.from(document.querySelectorAll('button'))
  .some(b => b.textContent.toUpperCase() === 'REGISTER');

// Se tem email input E NÃO tem register button → Page 4
if (hasEmailInput && !hasRegisterButton) {
  detectedPage = 4;
}
```

**Nota**: Pode estar combinada com Page 5 ou pode não existir em algumas localizações.

---

### ✅ **PÁGINA 5: Confirmation & Summary**

**Propósito**: Mostrar confirmação da renovação com detalhes

**Elementos Esperados**:
```html
<div class="confirmation">
  <h2>Renovação Confirmada!</h2>
  
  <p>Confirmação: <span id="confirmationNumber">CNF123456</span></p>
  <p>Placa: <span id="plate">ABC1234</span></p>
  <p>Localização: <span id="location">Downtown Lot</span></p>
  <p>Válido de: <span id="startTime">14:30</span></p>
  <p>Até: <span id="expiryTime">17:30</span></p>
  
  <button id="confirmBtn">Confirmar</button>
</div>
```

**Script de Coleta (em handlePage5)**:
```kotlin
val script = """
  (function(){
    try {
      // Coletar dados de confirmação
      const startTime = document.getElementById('startTime')?.innerText || 'N/A';
      const expiryTime = document.getElementById('expiryTime')?.innerText || 'N/A';
      const confirmationNumber = document.getElementById('confirmationNumber')?.innerText || 'N/A';
      const location = document.getElementById('location')?.innerText || 'N/A';
      
      const confirmationData = {
        startTime: startTime,
        expiryTime: expiryTime,
        confirmationNumber: confirmationNumber,
        location: location
      };
      
      // Notificar sucesso
      if (typeof Android !== 'undefined' && Android.onConfirmation) {
        Android.onConfirmation(JSON.stringify(confirmationData));
      }
      
      // Clicar botão de confirmação (se existir)
      const confirmBtn = document.getElementById('confirmBtn');
      if (confirmBtn) {
        confirmBtn.click();
      }
      
    } catch(e) {
      if (typeof Android !== 'undefined' && Android.onError) {
        Android.onError('Erro page 5: ' + e.message);
      }
    }
  })();
"""
```

**Detecção**:
```javascript
const hasEmailInput = document.getElementById('email') !== null;
const hasRegisterButton = Array.from(document.querySelectorAll('button'))
  .some(b => b.textContent.toUpperCase() === 'REGISTER');

// Se tem email input OU register button → Page 5
if (hasEmailInput || hasRegisterButton) {
  detectedPage = 5;
}
```

**Resultado Esperado**:
```kotlin
ConfirmationDetails(
  startTime = "14:30",
  expiryTime = "17:30",
  plate = "ABC1234",
  location = "Downtown Lot",
  confirmationNumber = "CNF123456"
)
```

---

## 🔍 Estratégia de Detecção de Página

```kotlin
fun captureAndProcessPage() {
  val script = """
    (function(){
      try {
        const hasPlateInput = document.getElementById('plate') !== null;
        const hasDurationButtons = Array.from(document.querySelectorAll('button'))
          .some(b => b.textContent.includes('Hour Parking'));
        const hasRegisterButton = Array.from(document.querySelectorAll('button'))
          .some(b => b.textContent.toUpperCase() === 'REGISTER');
        const hasEmailInput = document.getElementById('email') !== null;
        
        let detectedPage = 1;
        
        // Lógica de detecção (ordem importa!)
        if (hasEmailInput) {
          detectedPage = 5;
        } else if (hasRegisterButton) {
          detectedPage = 4;
        } else if (hasDurationButtons) {
          detectedPage = 3;
        } else if (hasPlateInput) {
          detectedPage = 2;
        }
        // senão Page 1
        
        return JSON.stringify({
          page: detectedPage,
          title: document.title,
          url: window.location.href,
          hasPlateInput: hasPlateInput,
          hasDurationButtons: hasDurationButtons,
          hasRegisterButton: hasRegisterButton,
          hasEmailInput: hasEmailInput
        });
      } catch(e) {
        return JSON.stringify({page: 0, error: e.message});
      }
    })();
  """
  
  webView.evaluateJavascript(script) { result →
    // Parse result e chama onPageReady(pageNumber)
  }
}
```

**Ordem de Verificação** (importante):
1. ✓ `hasEmailInput` → Page 5 (final)
2. ✓ `hasRegisterButton` → Page 4
3. ✓ `hasDurationButtons` → Page 3
4. ✓ `hasPlateInput` → Page 2
5. ✓ Nenhum → Page 1

---

## 🛠️ Tratamento de Falhas Comuns

### ❌ Problema: Elemento não encontrado

```javascript
// ❌ Ruim
const element = document.getElementById('plate');
element.value = 'ABC1234';  // Pode rebentar se null

// ✅ Bom
const element = document.getElementById('plate');
if (element) {
  element.value = 'ABC1234';
} else {
  throw new Error('Element not found: plate');
}
```

### ❌ Problema: Clique não dispara evento

```javascript
// ❌ Ruim
button.click();

// ✅ Bom
button.click();
button.dispatchEvent(new Event('click', { bubbles: true }));
button.dispatchEvent(new MouseEvent('click', {
  bubbles: true,
  cancelable: true,
  view: window
}));
```

### ❌ Problema: Elemento não é visível

```javascript
// ❌ Ruim
button.click();

// ✅ Bom
button.scrollIntoView({ behavior: 'smooth' });
setTimeout(() => {
  button.click();
}, 300);
```

### ❌ Problema: Valores de select diferentes

```javascript
// ❌ Ruim
select.value = 'Alberta';

// ✅ Bom - Matching de opções
const targetValue = 'Alberta';
const option = Array.from(select.options)
  .find(opt => opt.text.includes(targetValue) || opt.value === targetValue);
if (option) {
  select.value = option.value;
  select.dispatchEvent(new Event('change', { bubbles: true }));
}
```

---

## 📊 Timeouts e Delays

| Etapa | Delay | Motivo |
|-------|-------|--------|
| `webView.loadUrl()` → `onPageFinished()` | - | Automático |
| `onPageFinished()` → `captureAndProcessPage()` | 2000ms | Aguardar renderização |
| `captureAndProcessPage()` → `onPageReady()` | - | Imediato (JS sync) |
| `onPageReady()` → `handlePageN()` | 1000ms | Delay de segurança |
| `handlePageN()` → próxima página | 2000ms | Aguardar navegação |
| **Timeout Total** | **60000ms** | Proteção contra travamento |

---

## 🔐 Validações

```kotlin
// Validar placa
fun isValidLicensePlate(plate: String): Boolean {
  return plate.isNotEmpty() && 
         plate.length <= 10 &&
         plate.all { it.isLetterOrDigit() || it == '-' }
}

// Validar duração
fun isValidDuration(duration: String): Boolean {
  return duration in listOf("1 Hour", "2 Hour", "3 Hour", "4 Hour")
}

// Validar email
fun isValidEmail(email: String): Boolean {
  return email.contains("@") && email.contains(".")
}
```

---

## 🧪 Teste Manual (DebugActivity)

Para testar scripts antes de usar em automação:

1. **Abrir DebugActivity** (Debug Mode)
2. **Inserir URL**: `https://www.offstreet.io/location/LWLN9BUO`
3. **Clicar "GET INFO"** → Carrega página e coleta dados
4. **Analisar resultado JSON**:
   ```json
   {
     "page": 2,
     "title": "Vehicle Information",
     "url": "https://www.offstreet.io/...",
     "hasPlateInput": true,
     "hasDurationButtons": false,
     "hasRegisterButton": false,
     "hasEmailInput": false
   }
   ```
5. **Validar seletores**:
   - `document.getElementById('plate')` → deve retornar elemento
   - `document.getElementById('region')` → deve retornar select
   - etc.

---

## 📝 Checklist de Mudanças no Site

Se o site OffStreet for atualizado e automação falhar:

- [ ] Verificar se IDs dos elementos mudaram (`plate`, `region`, `email`, etc.)
- [ ] Verificar se classes CSS mudaram (`.duration-btn`, etc.)
- [ ] Verificar se textos de botões mudaram ("Next", "Continue", etc.)
- [ ] Verificar se há reCAPTCHA ou outro bloqueio
- [ ] Verificar se URL ou estrutura de navegação mudou
- [ ] Atualizar detectores em `captureAndProcessPage()`
- [ ] Atualizar scripts em `handlePageN()`
- [ ] Testar em DebugActivity antes de deploy

---

**Versão do Documento**: 1.0  
**Data**: Janeiro 8, 2026  
**Última Verificação**: https://www.offstreet.io/location/LWLN9BUO
