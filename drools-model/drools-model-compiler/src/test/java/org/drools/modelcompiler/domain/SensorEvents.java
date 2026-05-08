package org.drools.modelcompiler.domain;

public final class SensorEvents {

    private SensorEvents() { }

    public static final class MonitoringStation {
        private final String id;

        public MonitoringStation(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static final class SensorActivated {
        private final String sensorId;

        public SensorActivated(String sensorId) {
            this.sensorId = sensorId;
        }

        public String getSensorId() {
            return sensorId;
        }
    }

    public static final class HeartbeatOk {
        private final String sensorId;

        public HeartbeatOk(String sensorId) {
            this.sensorId = sensorId;
        }

        public String getSensorId() {
            return sensorId;
        }
    }

    public static final class AlarmRaised {
        private final String sensorId;
        private final String severity;

        public AlarmRaised(String sensorId, String severity) {
            this.sensorId = sensorId;
            this.severity = severity;
        }

        public String getSensorId() {
            return sensorId;
        }

        public String getSeverity() {
            return severity;
        }
    }

    public static final class CalibrationPassed {
        private final String sensorId;

        public CalibrationPassed(String sensorId) {
            this.sensorId = sensorId;
        }

        public String getSensorId() {
            return sensorId;
        }
    }

    public static final class OperatorAcknowledged {
        private final String sensorId;
        private final String operator;

        public OperatorAcknowledged(String sensorId, String operator) {
            this.sensorId = sensorId;
            this.operator = operator;
        }

        public String getSensorId() {
            return sensorId;
        }

        public String getOperator() {
            return operator;
        }
    }
}
