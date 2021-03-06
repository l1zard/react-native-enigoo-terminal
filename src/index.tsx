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

export function createCsobPayment(
  price: string,
  ipAddress: string,
  port: number,
  deviceId: string
): void {
  return EnigooTerminal.createCsobPayment(price, ipAddress, port, deviceId);
}

export function createCsobRefund(
  price: string,
  ipAddress: string,
  port: number,
  deviceId: string
): void {
  return EnigooTerminal.createCsobRefund(price, ipAddress, port, deviceId);
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
