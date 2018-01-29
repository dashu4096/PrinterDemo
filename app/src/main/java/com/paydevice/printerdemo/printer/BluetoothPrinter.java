package com.paydevice.printerdemo.printer;

import com.paydevice.printerdemo.printer.Printer;
import com.paydevice.printerdemo.printer.PrinterException;

/**
 * Created by hansen on 18-1-2.
 */

public class BluetoothPrinter implements Printer {
    @Override
    public void open() throws PrinterException {

    }

    @Override
    public void close() throws PrinterException {

    }

    @Override
    public int read(byte[] buf, int len) throws PrinterException {
        return 0;
    }

    @Override
    public void write(byte[] buf, int len) throws PrinterException {

    }

    @Override
    public int getType() {
        return PrinterCommon.BLUETOOTH_PRINTER;
    }
}
