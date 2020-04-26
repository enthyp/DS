import Ice, sys
import SmartHome

with Ice.initialize(sys.argv) as communicator:
    base = communicator.stringToProxy('SimplePrinter:default -p 10000')
    printer = SmartHome.PrinterPrx.checkedCast(base)
    if not printer:
        raise RuntimeError('Invalid proxy')
    
    printer.printString('Hello, Ice!')
