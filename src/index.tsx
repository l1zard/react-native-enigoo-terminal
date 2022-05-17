import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-enigoo-terminal' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: '- You have run \'pod install\'\n', default: '' }) +
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
    },
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
