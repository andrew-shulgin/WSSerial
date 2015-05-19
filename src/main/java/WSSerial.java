import jssc.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WSSerial extends WebSocketAdapter {
    private ConcurrentHashMap<String, SerialPort> serialPortTable;

    public WSSerial() {
        this.serialPortTable = new ConcurrentHashMap<>();
    }

    private void sendReply(Object result, String error, Long id) {
        JSONObject json = new JSONObject();
        json.put("result", result == null ? JSONObject.NULL : result);
        json.put("error", error == null ? JSONObject.NULL : error);
        json.put("id", id == null ? JSONObject.NULL : id);
        try {
            super.getRemote().sendString(json.toString());
        } catch (IOException e) {
            super.getRemote().sendStringByFuture(json.toString());
        }
    }

    private void sendRequest(String method, JSONArray params, Long id) {
        JSONObject json = new JSONObject();
        json.put("method", method == null ? JSONObject.NULL : method);
        json.put("params", params == null ? JSONObject.NULL : params);
        json.put("id", id == null ? JSONObject.NULL : id);
        try {
            super.getRemote().sendString(json.toString());
        } catch (IOException e) {
            super.getRemote().sendStringByFuture(json.toString());
        }
    }

    private void handleRequest(String method, JSONArray params, Long id) throws InvalidRequestException {
        String error;
        try {
            Object result;
            switch (method) {
                case "getPortNames":
                    result = getPortNames();
                    break;
                case "isOpened":
                    result = isOpened(params.getString(0));
                    break;
                case "open":
                    result = open(params.getString(0));
                    break;
                case "close":
                    result = close(params.getString(0));
                    break;
                case "setParams":
                    if (params.length() == 5) {
                        result = setParams(
                                params.getString(0),
                                params.getInt(1),
                                params.getInt(2),
                                params.getInt(3),
                                params.getInt(4)
                                          );
                    } else {
                        result = setParams(
                                params.getString(0),
                                params.getInt(1),
                                params.getInt(2),
                                params.getInt(3),
                                params.getInt(4),
                                params.getBoolean(5),
                                params.getBoolean(6)
                                          );
                    }
                    break;
                case "setRTS":
                    result = setRTS(params.getString(0), params.getBoolean(1));
                    break;
                case "setDTR":
                    result = setDTR(params.getString(0), params.getBoolean(1));
                    break;
                case "getInputBufferBytesCount":
                    result = getInputBufferBytesCount(params.getString(0));
                    break;
                case "getOutputBufferBytesCount":
                    result = getOutputBufferBytesCount(params.getString(0));
                    break;
                case "purgePort":
                    result = purgePort(params.getString(0), params.getInt(1));
                    break;
                case "getEventsMask":
                    result = getEventsMask(params.getString(0));
                    break;
                case "setEventsMask":
                    result = setEventsMask(params.getString(0), params.getInt(1));
                    break;
                case "getFlowControlMode":
                    result = getFlowControlMode(params.getString(0));
                    break;
                case "setFlowControlMode":
                    result = setFlowControlMode(params.getString(0), params.getInt(1));
                    break;
                case "write":
                    result = write(params.getString(0), DatatypeConverter.parseBase64Binary(params.getString(1)));
                    break;
                case "read":
                    byte[] data;
                    if (params.length() == 3) {
                        data = read(params.getString(0), params.getInt(1), params.getInt(2));
                    } else if (params.length() == 2) {
                        data = read(params.getString(0), params.getInt(1));
                    } else {
                        data = read(params.getString(0));
                    }
                    result = data != null ? DatatypeConverter.printBase64Binary(data) : null;
                    break;
                case "sendBreak":
                    result = sendBreak(params.getString(0), params.getInt(1));
                    break;
                case "getLinesStatus":
                    result = getLinesStatus(params.getString(0));
                    break;
                case "isCTS":
                    result = isCTS(params.getString(0));
                    break;
                case "isDSR":
                    result = isDSR(params.getString(0));
                    break;
                case "isRING":
                    result = isRING(params.getString(0));
                    break;
                case "isRLSD":
                    result = isRLSD(params.getString(0));
                    break;
                default:
                    throw new InvalidRequestException();
            }
            sendReply(result, null, id);
            return;
        } catch (SerialPortException serialEx) {
            error = serialEx.getExceptionType();
        } catch (SerialPortTimeoutException serialTimeoutEx) {
            error = "Serial port timeout";
        }
        sendReply(null, error, id);
    }

    private SerialPort getSerialPort(String portName) throws SerialPortException {
        SerialPort serialPort = this.serialPortTable.get(portName);
        if (serialPort == null) {
            throw new SerialPortException(portName, null, "Port not opened");
        }
        return serialPort;
    }

    private String[] getPortNames() {
        return SerialPortList.getPortNames();
    }

    private boolean isOpened(String portName) throws SerialPortException {
        try {
            return getSerialPort(portName).isOpened();
        } catch (SerialPortException ex) {
            if (ex.getExceptionType().equals(SerialPortException.TYPE_PORT_NOT_OPENED)) {
                return false;
            } else throw ex;
        }
    }

    private boolean open(String portName) throws SerialPortException {
        boolean success = false;
        final SerialPort serialPort = new SerialPort(portName);
        if (serialPort.openPort()) {
            serialPort.addEventListener(new Reader());
            this.serialPortTable.put(portName, serialPort);
            success = true;
        }
        return success;
    }

    private boolean close(String portName) throws SerialPortException {
        return getSerialPort(portName).closePort();
    }

    private boolean setParams(String portName, int baudRate, int dataBits, int stopBits, int parity, boolean setRTS,
                              boolean setDTR) throws SerialPortException {
        return getSerialPort(portName).setParams(baudRate, dataBits, stopBits, parity, setRTS, setDTR);
    }

    private boolean setParams(String portName, int baudRate, int dataBits, int stopBits, int parity) throws
    SerialPortException {
        return setParams(portName, baudRate, dataBits, stopBits, parity, true, true);
    }

    private boolean setRTS(String portName, boolean enabled) throws SerialPortException {
        return getSerialPort(portName).setRTS(enabled);
    }

    private boolean setDTR(String portName, boolean enabled) throws SerialPortException {
        return getSerialPort(portName).setDTR(enabled);
    }

    private int getInputBufferBytesCount(String portName) throws SerialPortException {
        return getSerialPort(portName).getInputBufferBytesCount();
    }

    private int getOutputBufferBytesCount(String portName) throws SerialPortException {
        return getSerialPort(portName).getOutputBufferBytesCount();
    }

    private boolean purgePort(String portName, int flags) throws SerialPortException {
        return getSerialPort(portName).purgePort(flags);
    }

    private int getEventsMask(String portName) throws SerialPortException {
        return getSerialPort(portName).getEventsMask();
    }

    private boolean setEventsMask(String portName, int mask) throws SerialPortException {
        return getSerialPort(portName).setEventsMask(mask);
    }

    private int getFlowControlMode(String portName) throws SerialPortException {
        return getSerialPort(portName).getFlowControlMode();
    }

    private boolean setFlowControlMode(String portName, int mask) throws SerialPortException {
        return getSerialPort(portName).setFlowControlMode(mask);
    }

    private boolean write(String portName, byte[] buffer) throws SerialPortException {
        return getSerialPort(portName).writeBytes(buffer);
    }

    private byte[] read(String portName, int count, int timeout) throws SerialPortException,
    SerialPortTimeoutException {
        return getSerialPort(portName).readBytes(count, timeout);
    }

    private byte[] read(String portName, int count) throws SerialPortException, SerialPortTimeoutException {
        return getSerialPort(portName).readBytes(count);
    }

    private byte[] read(String portName) throws SerialPortException, SerialPortTimeoutException {
        return getSerialPort(portName).readBytes();
    }

    private boolean sendBreak(String portName, int duration) throws SerialPortException {
        return getSerialPort(portName).sendBreak(duration);
    }

    private int[] getLinesStatus(String portName) throws SerialPortException {
        return getSerialPort(portName).getLinesStatus();
    }

    private boolean isCTS(String portName) throws SerialPortException {
        return getSerialPort(portName).isCTS();
    }

    private boolean isDSR(String portName) throws SerialPortException {
        return getSerialPort(portName).isDSR();
    }

    private boolean isRING(String portName) throws SerialPortException {
        return getSerialPort(portName).isRING();
    }

    private boolean isRLSD(String portName) throws SerialPortException {
        return getSerialPort(portName).isRLSD();
    }

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        Runnable ping = new Runnable() {
            @Override
            public void run() {
                try {
                    WSSerial.super.getRemote().sendPing(ByteBuffer.wrap("PING".getBytes()));
                } catch (IOException ignored) {
                }
            }
        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(ping, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        Long id = null;
        try {
            JSONObject json = new JSONObject(message);
            String method = json.getString("method");
            JSONArray params = json.getJSONArray("params");
            id = json.getLong("id");
            handleRequest(method, params, id);
        } catch (JSONException | InvalidRequestException e) {
            sendReply(null, "Invalid request", id);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        for (Map.Entry<String, SerialPort> entry : serialPortTable.entrySet()) {
            try {
                close(entry.getKey());
            } catch (SerialPortException ignored) {
            }
        }
        System.exit(0);
    }

    private class Reader implements SerialPortEventListener {
        @Override
        public void serialEvent(SerialPortEvent event) {
            if ((event.isRXCHAR()) && (event.getEventValue() == 0)) {
                return;
            }
            JSONArray params = new JSONArray();
            params.put(event.getPortName());
            params.put(event.getEventType());
            params.put(event.getEventValue());
            WSSerial.this.sendRequest("serialEvent", params, null);
        }
    }

    private class InvalidRequestException extends Exception {
        public InvalidRequestException() { }
    }
}
