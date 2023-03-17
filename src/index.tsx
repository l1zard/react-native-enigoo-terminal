import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-enigoo-terminal' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const EnigooTerminal = NativeModules.EnigooTerminal
  ? NativeModules.EnigooTerminal
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function initCsobConnection(
  ipAddress: string,
  port: string,
  deviceId: string
): void {
  return EnigooTerminal.initCsobConnection(ipAddress, port, deviceId);
}

export function createCsobGetAppInfo(): void {
  return EnigooTerminal.createCsobGetAppInfo();
}

export function createCsobPayment(price: string): void {
  return EnigooTerminal.createCsobPayment(price);
}

export function createCsobRefund(price: string): void {
  return EnigooTerminal.createCsobRefund(price);
}

export function createCsobCloseTotals(): void {
  return EnigooTerminal.createCsobCloseTotals();
}

export function createCsobHandshake(): void {
  return EnigooTerminal.createCsobHandshake();
}

export function createCsobTmsBCall(): void {
  return EnigooTerminal.createCsobTmsBCall();
}

export function createCsobTmsNCall(): void {
  return EnigooTerminal.createCsobTmsNCall();
}

export function createFiscalProPayment(
  price: string,
  orderId: string,
  ipAddress: string,
  port: number
): void {
  return EnigooTerminal.createFiscalProPayment(price, orderId, ipAddress, port);
}

export function createFiscalProRefund(
  price: string,
  orderId: string,
  ipAddress: string,
  port: number
): void {
  return EnigooTerminal.createFiscalProRefund(price, orderId, ipAddress, port);
}

export function setUsbServiceFiskalProSk(): void {
  return EnigooTerminal.setUsbServiceFiskalProSk();
}

export function createFiskalProSkTerminalRecord(data: string): void {
  return EnigooTerminal.createFiskalProSkTerminalRecord(data);
}
