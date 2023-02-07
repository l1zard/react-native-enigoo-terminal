# Terminál dokumentace

# ČSOB

## Vytvoření platby

```java
createCsobPayment(String price, String ipAddress, int port, String deviceId)
```

Vytvoří platbu na platební terminál. Cena musí být string ve tvaru např.:  “100” nebo “100.0”.

Po vytvoření platby terminál čeká na zaplacení po dobu 1. minuty poté platbu vystornuje.

### Refundace platby

```java
createCsobRefund(String price, String ipAddress, int port, String deviceId)
```

Vytvoří refundaci na platební terminál

### Uzávěrka terminálu
```java
createCsobCloseTotals(String ipAddress, int port, String deviceId)
```
### Test spojení
```java
createCsobHandshake(String ipAddress, int port, String deviceId)
```

### Aktualizace
```java
createCsobTmsBCall(String ipAddress, int port, String deviceId)
```

```java
createCsobTmsNCall(String ipAddress, int port, String deviceId)
```
# FiskalPro

## Vytvoření platby

```java
createFiscalProPayment(String price, String orderId, String ipAddress, int port)
```

Vytvoří platbu na platební terminál. Cena musí být string ve tvaru např.:  “100” nebo “100.0”.

Po vytvoření platby terminál čeká na zaplacení po dobu 1. minuty poté platbu vystornuje.

### Refundace platby

```java
createFiscalProRefund(String price, String orderId, String ipAddress, int port)
```

Vytvoří refundaci na platební terminál pouze na původní objednávku!

# FiskalPro - SK

```java
createFiskalProSkTerminalRecord(String data)
```

Vytvoří platbu na terminal Fiskal Pro - SK pro evidenci tržeb.

parametr data je JSON string kde:

```json
{
  "paymentType": 1,
  "items": [
    {
      "name": "Vstupenka",
      "count": 2,
      "singlePrice": 10,
      "vat": 1
    }
  ]
}
```

**paymentType**

- 1 = Hotově
- 2 = Kartou

**vat**

- 1 = 20%
- 2= 10%
- 3 = 15%
- 4 =  default (asi 0)

**Po dokončení platby přijde status**

```json
{
	type: "FISKAL_PAYMENT"
	status: "SUCCESS"
}
```

# Obecné stavy

## Terminal Listener

```jsx
DeviceEventEmitter.addListener("TERMINAL_EVENTS", (data) => {})
```

### Vytvoření platby **📞**

```json
{
	type: "CREATE_PAYMENT",
	status: "SUCCESS"
}
```

### Dokončení platby - po zaplacení **📞**

```json
{
  type: "PURCHASE",
  status: "SUCCESS",
  merchantRecipe: [
    "row1",
    "row2",
    "row3",
    ...
  ],
  customerRecipe: [
    "row1",
    "row2",
    "row3",
    ...
  ]
}
```

Po dokončení transakce vždy přijde ve výsledku **merchantRecipe** a **customerRecipe**, v případě,
že terminál vyžaduje po pokladně vytištění lístečku, tak v těchto polích budou jednotlivé řádky účtenek.
V případě přázdných polí se po pokladně nevyžaduje tisk lístečků.

Může taktéž nastat situace, že je naplněno pouze pole merchantRecipe - nastává například při uzávěrce
či testu spojení, kdy je výsledek operace určen pouze pro obchodníka.

### Připojení

Po každém zavolání funkce přijde emit connection

```json
{
	type: "CONNECTION"
	status: "SUCCESS"
}
```

- Stav

  `SUCCESS`  - Všechno proběhlo v pořádku

  `FAILED`  - Připojení se nezdařilo, špatně nastavený terminál nebo připojení na něj


### Seznam všech typů a stavů

- Typ

  `CREATE_PAYMENT`  - Vytvoření platby

  `CREATE_REFUND`  - Vytvoření refundace

  `PURCHASE`  - Platba

  `RETURN`  - Vrácení prostředků na kartu

  `REVERSAL`  - slouží ke zrušení poslední podpisové transakce provedené na platebním terminálu, v případě nesouhlasu podpisu. Může být proveden pouze bezprostředně po platební transakci a mezi platební transakcí a jejím reversalem nesmí být provedena uzávěrka.

  `CLOSE_TOTALS`  - Uzávěrka platebního terminálu.

  `HANDSHAKE`  - test spojení.

  `TMS_CALL`  - aktualizace terminálu.

- Stav

  `SUCCESS`  - Všechno proběhlo v pořádku

  `DEFAULT_ERROR`  - Chyba s kartou nebo terminálem

  `CANCEL`  - Zrušeno uživatelem nebo chybný pin

  `CARD_ERROR`  - Chyba s kartou

  `CARD_EXPIRED`  - Karta je expirovaná

  `CARD_YOUNG`  - Karta ještě nezačala platit

  `CARD_NO_ENOUGH_MONEY`  - Nedostatek prostředků na kartě

  `TIMEOUT`  - Čas pro transakci vypršel

  `CARD_BLOCKED`  - Karta je zablokovaná
