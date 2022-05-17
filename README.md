# ČSOB - Terminál dokumentace

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

## Terminal Listener

```jsx
DeviceEventEmitter.addListener("TERMINAL_EVENTS", (data) => {})
```

### Vytvoření platby **📞**

```json
{
	type: "CREATE_PAYMENT"
	status: "SUCCESS"
}
```

### Dokončení platby - po zaplacení **📞**

```json
{
	type: "PURCHASE"
	status: "SUCCESS"
}
```

### Seznam všech typů a stavů

- Typ

  `CREATE_PAYMENT`  - Vytvoření platby

  `CREATE_REFUND`  - Vytvoření refundace

  `PURCHASE`  - Platba

  `RETURN`  - Vrácení prostředků na kartu

  `REVERSAL`  - slouží ke zrušení poslední podpisové transakce provedené na platebním terminálu, v případě nesouhlasu podpisu. Může být proveden pouze bezprostředně po platební transakci a mezi platební transakcí a jejím reversalem nesmí být provedena uzávěrka.

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
