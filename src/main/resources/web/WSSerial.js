var WSSerial = {
    webSocket: null,
    pendingRequests: [],
    BAUDRATE_110: 110,
    BAUDRATE_115200: 115200,
    BAUDRATE_1200: 1200,
    BAUDRATE_128000: 128000,
    BAUDRATE_14400: 14400,
    BAUDRATE_19200: 19200,
    BAUDRATE_256000: 256000,
    BAUDRATE_300: 300,
    BAUDRATE_38400: 38400,
    BAUDRATE_4800: 4800,
    BAUDRATE_57600: 57600,
    BAUDRATE_600: 600,
    BAUDRATE_9600: 9600,
    DATABITS_5: 5,
    DATABITS_6: 6,
    DATABITS_7: 7,
    DATABITS_8: 8,
    ERROR_FRAME: 8,
    ERROR_OVERRUN: 2,
    ERROR_PARITY: 4,
    FLOWCONTROL_NONE: 0,
    FLOWCONTROL_RTSCTS_IN: 1,
    FLOWCONTROL_RTSCTS_OUT: 2,
    FLOWCONTROL_XONXOFF_IN: 4,
    FLOWCONTROL_XONXOFF_OUT: 8,
    MASK_BREAK: 64,
    MASK_CTS: 8,
    MASK_DSR: 16,
    MASK_ERR: 128,
    MASK_RING: 256,
    MASK_RLSD: 32,
    MASK_RXCHAR: 1,
    MASK_RXFLAG: 2,
    MASK_TXEMPTY: 4,
    PARITY_EVEN: 2,
    PARITY_MARK: 3,
    PARITY_NONE: 0,
    PARITY_ODD: 1,
    PARITY_SPACE: 4,
    PURGE_RXABORT: 2,
    PURGE_RXCLEAR: 8,
    PURGE_TXABORT: 1,
    PURGE_TXCLEAR: 4,
    STOPBITS_1: 1,
    STOPBITS_1_5: 3,
    STOPBITS_2: 2,
    BREAK: 64,//0
    CTS: 8,//state of CTS line (0 - OFF, 1 - ON)
    DSR: 16,//state of DSR line (0 - OFF, 1 - ON)
    ERR: 128,//mask of errors
    RING: 256,//state of RING line (0 - OFF, 1 - ON)
    RLSD: 32,//state of RLSD line (0 - OFF, 1 - ON)
    RXCHAR: 1,//bytes count in input buffer
    RXFLAG: 2,//bytes count in input buffer (Not supported in Linux)
    TXEMPTY: 4,//bytes count in output buffer
    errorHandler: function (error) {
        console.error(error)
    },
    onBREAK: function (port) {
    },
    onCTS: function (port) {
    },
    onDSR: function (port) {
    },
    onERR: function (port) {
    },
    onRING: function (port) {
    },
    onRLSD: function (port) {
    },
    onRXCHAR: function (port, value) {
    },
    onRXFLAG: function (port) {
    },
    onTXEMPTY: function (port) {
    }
};
WSSerial.command = function (method, params, callback) {
    if (this.webSocket.readyState !== WebSocket.OPEN) {
        this.errorHandler("WebSocket not open");
        return;
    }
    var id = Date.now(),
        command = {method: method, params: params, id: id};
    this.pendingRequests.push({id: id, callback: callback});
    this.webSocket.send(JSON.stringify(command));
};
WSSerial.process = function (message) {
    var data = JSON.parse(message.data);
    if (data.hasOwnProperty("method")) {
        if (data.method === "serialEvent") {
            var port = data.params[0],
                event = data.params[1],
                value = data.params[2];
            switch (event) {
                case WSSerial.BREAK:
                    WSSerial.onBREAK(port);
                    break;
                case WSSerial.CTS:
                    WSSerial.onCTS(port);
                    break;
                case WSSerial.DSR:
                    WSSerial.onDSR(port);
                    break;
                case WSSerial.ERR:
                    WSSerial.onERR(port);
                    break;
                case WSSerial.RING:
                    WSSerial.onRING(port);
                    break;
                case WSSerial.RLSD:
                    WSSerial.onRLSD(port);
                    break;
                case WSSerial.RXCHAR:
                    WSSerial.onRXCHAR(port, value);
                    break;
                case WSSerial.RXFLAG:
                    WSSerial.onRXFLAG(port);
                    break;
                case WSSerial.TXEMPTY:
                    WSSerial.onTXEMPTY(port);
                    break;
            }
        }
    } else {
        var id = data.id,
            result = data.result,
            error = data.error;
        if (error === null) {
            for (var i = 0; i < WSSerial.pendingRequests.length; i++) {
                if (WSSerial.pendingRequests[i].id === id) {
                    WSSerial.pendingRequests[i].callback(result);
                }
            }
        } else
            WSSerial.errorHandler(error);
    }
};
WSSerial.connect = function (onconnected) {
    onconnected = typeof onconnected === "function" ? onconnected : function () {
    };
    var protocol = location.protocol === "http:" ? "ws:" : "wss:",
        host = "localhost",
        port = protocol === "ws:" ? "8080" : "8443",
        uri = "ws";
    this.webSocket = new WebSocket(protocol + "//" + host + ":" + port + "/" + uri);
    this.webSocket.onopen = onconnected;
    this.webSocket.onmessage = this.process;
};
WSSerial.disconnect = function (ondisconnected) {
    ondisconnected = typeof ondisconnected === "function" ? ondisconnected : function () {
    };
    this.webSocket.onclose = ondisconnected;
    this.webSocket.close();
};
WSSerial.getPortNames = function (callback) {
    this.command("getPortNames", [], callback);
};
WSSerial.isOpened = function (portName, callback) {
    this.command("isOpened", [portName], callback);
};
WSSerial.open = function (portName, callback) {
    this.command("open", [portName], callback);
};
WSSerial.close = function (portName, callback) {
    this.command("close", [portName], callback);
};
WSSerial.setParams = function (portName, baudRate, dataBits, stopBits, parity, callback) {
    this.command("setParams", [portName, baudRate, dataBits, stopBits, parity], callback);
};
WSSerial.setParamsRtsDtr = function (portName, baudRate, dataBits, stopBits, parity, setRTS, setDTR, callback) {
    this.command("setParams", [portName, baudRate, dataBits, stopBits, parity, setRTS, setDTR], callback);
};
WSSerial.setRTS = function (portName, on, callback) {
    this.command("setRTS", [portName, on], callback);
};
WSSerial.setDTR = function (portName, on, callback) {
    this.command("setDTR", [portName, on], callback);
};
WSSerial.getInputBufferBytesCount = function (portName, callback) {
    this.command("getInputBufferBytesCount", [portName], callback);
};
WSSerial.getOutputBufferBytesCount = function (portName, callback) {
    this.command("getOutputBufferBytesCount", [portName], callback);
};
WSSerial.purgePort = function (portName, mask, callback) {
    this.command("purgePort", [portName, mask], callback);
};
WSSerial.getEventsMask = function (portName, callback) {
    this.command("getEventsMask", [portName], callback);
};
WSSerial.setEventsMask = function (portName, mask, callback) {
    this.command("setEventsMask", [portName, mask], callback);
};
WSSerial.getFlowControlMode = function (portName, callback) {
    this.command("getFlowControlMode", [portName], callback);
};
WSSerial.setFlowControlMode = function (portName, mask, callback) {
    this.command("setFlowControlMode", [portName, mask], callback);
};
WSSerial.write = function (portName, data, callback) {
    this.command("write", [portName, btoa(data)], callback);
};
WSSerial.read = function (portName, callback) {
    this.command("read", [portName], function (data) {
        callback(data !== null ? atob(data.replace(/\s/g, '')) : null);
    });
};
WSSerial.readBytes = function (portName, count, callback) {
    this.command("read", [portName, count], function (data) {
        callback(data !== null ? atob(data.replace(/\s/g, '')) : null);
    });
};
WSSerial.readBytesTimeout = function (portName, count, timeout, callback) {
    this.command("read", [portName, count, timeout], function (data) {
        callback(data !== null ? atob(data.replace(/\s/g, '')) : null);
    });
};
WSSerial.sendBreak = function (portName, duration, callback) {
    this.command("sendBreak", [portName, duration], callback);
};
WSSerial.getLinesStatus = function (portName, callback) {
    this.command("getLinesStatus", [portName], callback);
};
WSSerial.isCTS = function (portName, callback) {
    this.command("isCTS", [portName], callback);
};
WSSerial.isDSR = function (portName, callback) {
    this.command("isDSR", [portName], callback);
};
WSSerial.isRING = function (portName, callback) {
    this.command("isRING", [portName], callback);
};
WSSerial.isRLSD = function (portName, callback) {
    this.command("isRLSD", [portName], callback);
};