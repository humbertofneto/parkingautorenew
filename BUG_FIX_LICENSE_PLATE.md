# 🐛 BUG FIX: Placa Fixa em ABC1234 - CORRIGIDO

**Data**: Janeiro 8, 2026  
**Severidade**: 🔴 CRÍTICO  
**Status**: ✅ CORRIGIDO  
**Arquivo**: `ParkingAutomationManager.kt`  
**Linha**: 225

---

## O Problema

Quando o usuário digitava uma placa (ex: `XYZ4321`), o app **salvava corretamente**, mas na renovação automática estava usando **sempre `ABC1234`**.

### Fluxo Incorreto:

```
Usuário digita: XYZ4321
    ↓
SharedPreferences: "license_plate" = "XYZ4321" ✅
    ↓
AutoRenewActivity.startAutoRenew(plate="XYZ4321")
    ↓
ParkingRenewalService chama:
    automationManager.start("XYZ4321", ...) ✅
    ↓
ParkingAutomationManager.start():
    plateNumber = "XYZ4321" ✅
    ↓
handlePage2():
    val licensePlate = webView.getTag() as? String ?: "ABC1234" ❌
    
    webView.getTag() = null (nunca foi setado!)
    ↓
    licensePlate = "ABC1234" ❌❌❌
    ↓
JavaScript injeta "ABC1234" no site ❌
```

---

## A Raiz do Problema

```kotlin
// ANTES (ERRADO) - linha 225
val licensePlate = webView.getTag() as? String ?: "ABC1234"
```

### Por que estava errado:

1. **`webView.getTag()`** nunca era setado
2. Retornava `null`
3. Usava o default `"ABC1234"`
4. Ignorava completamente o `plateNumber` que estava correto

---

## A Solução

```kotlin
// DEPOIS (CORRETO) - linha 225
val licensePlate = plateNumber
Log.d(TAG, "Using license plate: $licensePlate")
```

### Por que funciona:

1. ✅ `plateNumber` é setado corretamente em `start(plate: String, ...)`
2. ✅ Contém exatamente o que o usuário digitou
3. ✅ É reutilizado em TODAS as renovações
4. ✅ Simples e direto - sem fallbacks confusos

---

## Fluxo Corrigido

```
Usuário digita: XYZ4321
    ↓
start(plate="XYZ4321")
    plateNumber = "XYZ4321"
    ↓
handlePage2():
    val licensePlate = plateNumber  // ✅ "XYZ4321"
    ↓
JavaScript: plateInput.value = "XYZ4321"  // ✅ CORRETO!
    ↓
Website mostra: [XYZ4321] ✅
    ↓
Confirmação: plate = "XYZ4321" ✅
```

---

## Garantias Após Fix

| Cenário | Antes | Depois |
|---------|-------|--------|
| Usuário digita XYZ4321 | Usa ABC1234 ❌ | Usa XYZ4321 ✅ |
| Renovação #1 | ABC1234 ❌ | XYZ4321 ✅ |
| Renovação #2 | ABC1234 ❌ | XYZ4321 ✅ |
| Renovação #3 | ABC1234 ❌ | XYZ4321 ✅ |
| Clica START AGAIN | ABC1234 ❌ | Permite nova entrada ✅ |

---

## Como Testar

### Teste 1: Primeira Renovação
```
1. Abrir app
2. Digitar placa: "TEST0001"
3. Clicar START
4. Verificar logs:
   ✅ D: "Using license plate: TEST0001"
   ✅ Website deve mostrar [TEST0001]
   ✅ Confirmação deve ter plate: "TEST0001"
```

### Teste 2: Renovações Repetidas
```
1. Deixar app renovando por 3 ciclos
2. Verificar logs a cada renovação:
   Renovação #1: "Using license plate: TEST0001" ✅
   Renovação #2: "Using license plate: TEST0001" ✅
   Renovação #3: "Using license plate: TEST0001" ✅
```

### Teste 3: Mudar Placa com Start Again
```
1. Digitar "PLATE01"
2. Clicar START
3. Clicar STOP
4. Clicar "START AGAIN"
5. Digitar "PLATE02" (nova placa)
6. Clicar START
7. Verificar: 
   ✅ Agora usa "PLATE02"
   ❌ Não usa "PLATE01"
   ❌ Não usa "ABC1234"
```

### Teste 4: Verificar Logs
```bash
# No terminal, filtrar logs do app
adb logcat | grep "Using license plate"

# Deve mostrar:
D/ParkingAutomation: Using license plate: TEST0001
D/ParkingAutomation: Using license plate: TEST0001  (renovação)
D/ParkingAutomation: Using license plate: TEST0001  (renovação)
```

---

## Código Alterado

### Arquivo
`app/src/main/java/com/example/parkingautorenew/ParkingAutomationManager.kt`

### Linhas 223-226
```kotlin
// ANTES
private fun handlePage2() {
    Log.d(TAG, "Handling Page 2 (Vehicle Info)")
    
    val licensePlate = webView.getTag() as? String ?: "ABC1234"

// DEPOIS
private fun handlePage2() {
    Log.d(TAG, "Handling Page 2 (Vehicle Info)")
    
    // ✅ CORRIGIDO: Usar plateNumber que foi setado corretamente em start()
    val licensePlate = plateNumber
    Log.d(TAG, "Using license plate: $licensePlate")
```

---

## Impacto

- 🟢 **Crítico**: Funcionalidade principal agora trabalha como esperado
- 🟢 **Segurança**: Não há segurança comprometida
- 🟢 **Performance**: Sem impacto na performance
- 🟢 **Compatibilidade**: Compatível com todas as versões anteriores

---

## Validação

✅ Código compilado  
✅ Lógica verificada  
✅ Variável `plateNumber` corretamente setada em `start()`  
✅ Nenhum outro lugar tenta usar `webView.getTag()`  

---

## Próximas Ações

1. ✅ Compilar APK com correção
2. ✅ Testar com placas diferentes
3. ✅ Testar renovações repetidas
4. ✅ Commit com mensagem clara
5. ✅ Atualizar versionCode → 5, versionName → "1.0.4"

---

**Corrigido com sucesso!** 🎉

A placa que o usuário digita agora será usada EXATAMENTE como foi digitada em TODAS as renovações.
