/*
 * Copyright (c) 2017 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ftclib;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

import trclib.TrcAccelerometer;
import trclib.TrcDbgTrace;
import trclib.TrcGyro;
import trclib.TrcRobot;
import trclib.TrcTaskMgr;
import trclib.TrcUtil;

/**
 * This class implements the BNO055 IMU which is actually an Adafruit BNO055. It encapsulates two sub-classes:
 * a 3-axis gyro and a 3-axis accelerometer.
 */
public class FtcBNO055Imu
{
    private static final String moduleName = "FtcBNO055Imu";
    private static final boolean debugEnabled = false;
    private static final boolean tracingEnabled = false;
    private static final TrcDbgTrace.TraceLevel traceLevel = TrcDbgTrace.TraceLevel.API;
    private static final TrcDbgTrace.MsgLevel msgLevel = TrcDbgTrace.MsgLevel.INFO;
    private TrcDbgTrace dbgTrace = null;

    private static final boolean USE_QUATERNION = true;

    /**
     * This class implements the gyro part of hte BNO055 IMU. It extends TrcGyro so that it implements the standard
     * gyro interface.
     */
    private class Gyro extends TrcGyro
    {
        private final TrcTaskMgr.TaskObject gyroTaskObj;
        private boolean taskEnabled = false;
        private double timestamp = 0.0;
        private double xAngle = 0.0, yAngle = 0.0, zAngle = 0.0;
        private double xRotationRate = 0.0, yRotationRate = 0.0, zRotationRate = 0.0;

        /**
         * Constructor: Creates an instance of the object.
         *
         * @param instanceName specifies the instance name.
         */
        private Gyro(String instanceName)
        {
            //
            // BNO055 IMU has a 3-axis gyro. The angular orientation data it returns is in Ordinal system.
            // So we need to convert it to Cartesian system.
            //
            super(instanceName, 3,
                    GYRO_HAS_X_AXIS | GYRO_HAS_Y_AXIS | GYRO_HAS_Z_AXIS | GYRO_CONVERT_TO_CARTESIAN, null);
            gyroTaskObj = TrcTaskMgr.getInstance().createTask(instanceName, this::gyroTask);
        }   //Gyro

        /**
         * This method enables/disables the gyro task that reads and caches the gyro data periodically.
         *
         * @param enabled specifies true for enabling the gyro task, disabling it otherwise.
         */
        public synchronized void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);

            if (enabled)
            {
                gyroTaskObj.registerTask(TrcTaskMgr.TaskType.STANDALONE_TASK, 50);
            }
            else
            {
                gyroTaskObj.unregisterTask(TrcTaskMgr.TaskType.STANDALONE_TASK);
            }

            taskEnabled = enabled;
        }   //setEnabled

        /**
         * This method returns the state of the gyro task.
         *
         * @return true if gyro task is enabled, false otherwise.
         */
        public synchronized boolean isEnabled()
        {
            return taskEnabled;
        }   //isEnabled

        /**
         * This method is called periodically to read the gyro data.
         *
         * @param taskType specifies the type of task being run.
         * @param runMode specifies the competition mode that is running.
         */
        private synchronized void gyroTask(TrcTaskMgr.TaskType taskType, TrcRobot.RunMode runMode)
        {
            timestamp = TrcUtil.getCurrentTime();
            //
            // The Z-axis returns positive heading in the anticlockwise direction, so we must negate it for
            // our convention.
            //
            if (USE_QUATERNION)
            {
                Quaternion q = imu.getQuaternionOrientation();
                double sinp = 2.0 * (q.w * q.y - q.z * q.x);

                xAngle = Math.toDegrees(Math.atan2(2.0 * (q.w * q.x + q.y * q.z), 1.0 - 2.0 * (q.x * q.x + q.y * q.y)));
                yAngle = Math.toDegrees(Math.abs(sinp) >= 1.0 ? Math.signum(sinp) * (Math.PI / 2.0) : Math.asin(sinp));
                zAngle = -Math.toDegrees(Math.atan2(2.0 * (q.w * q.z + q.x * q.y), 1.0 - 2.0 * (q.y * q.y + q.z * q.z)));
            }
            else
            {
                Orientation orientation = imu.getAngularOrientation(
                        AxesReference.INTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES);
                xAngle = orientation.firstAngle;
                yAngle = orientation.secondAngle;
                zAngle = -orientation.thirdAngle;
            }

            AngularVelocity angularVelocity = imu.getAngularVelocity();
            xRotationRate = angularVelocity.xRotationRate;
            yRotationRate = angularVelocity.yRotationRate;
            zRotationRate = angularVelocity.zRotationRate;
        }   //gyroTask

        //
        // Implements TrcGyro abstract methods.
        //

        /**
         * This method returns the raw data of the specified type for the x-axis.
         *
         * @param dataType specifies the data type.
         * @return raw data of the specified type for the x-axis.
         */
        @Override
        public synchronized SensorData<Double> getRawXData(DataType dataType)
        {
            final String funcName = "getRawXData";
            double value = 0.0;

            if (dataType == DataType.ROTATION_RATE)
            {
                value = xRotationRate;
            }
            else if (dataType == DataType.HEADING)
            {
                value = xAngle;
            }
            SensorData<Double> data = new SensorData<>(TrcUtil.getCurrentTime(), value);

            if (debugEnabled)
            {
                dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
                dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                        "=(timestamp:%.3f,value:%f", data.timestamp, data.value);
            }

            return data;
        }   //getRawXData

        /**
         * This method returns the raw data of the specified type for the y-axis.
         *
         * @param dataType specifies the data type.
         * @return raw data of the specified type for the y-axis.
         */
        @Override
        public synchronized SensorData<Double> getRawYData(DataType dataType)
        {
            final String funcName = "getRawYData";
            double value = 0.0;

            if (dataType == DataType.ROTATION_RATE)
            {
                value = yRotationRate;
            }
            else if (dataType == DataType.HEADING)
            {
                value = yAngle;
            }
            SensorData<Double> data = new SensorData<>(TrcUtil.getCurrentTime(), value);

            if (debugEnabled)
            {
                dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
                dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                        "=(timestamp:%.3f,value:%f", data.timestamp, data.value);
            }

            return data;
        }   //getRawYData

        /**
         * This method returns the raw data of the specified type for the z-axis.
         *
         * @param dataType specifies the data type.
         * @return raw data of the specified type for the z-axis.
         */
        @Override
        public synchronized SensorData<Double> getRawZData(DataType dataType)
        {
            final String funcName = "getRawZData";
            double value = 0.0;

            if (dataType == DataType.ROTATION_RATE)
            {
                value = zRotationRate;
            }
            else if (dataType == DataType.HEADING)
            {
                value = zAngle;
            }
            SensorData<Double> data = new SensorData<>(TrcUtil.getCurrentTime(), value);

            if (debugEnabled)
            {
                dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
                dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                        "=(timestamp:%.3f,value:%f", data.timestamp, data.value);
            }

            return data;
        }   //getRawZData

    }   //class Gyro

    /**
     * This class implements the accelerometer part of hte BNO055 IMU. It extends TrcAccelerometer so that it
     * implements the standard accelerometer interface.
     */
    private class Accelerometer extends TrcAccelerometer
    {
        private TrcTaskMgr.TaskObject accelTaskObj;
        private boolean taskEnabled = false;
        private double timestamp = 0.0;
        private double xAccel = 0.0, yAccel = 0.0, zAccel = 0.0;
        private double xVel = 0.0, yVel = 0.0, zVel = 0.0;
        private double xPos = 0.0, yPos = 0.0, zPos = 0.0;

        /**
         * Constructor: Creates an instance of the object.
         *
         * @param instanceName specifies the instance name.
         */
        private Accelerometer(String instanceName)
        {
            //
            // BNO055 IMU has a 3-axis accelerometer.
            //
            super(instanceName, 3, ACCEL_HAS_X_AXIS | ACCEL_HAS_Y_AXIS | ACCEL_HAS_Z_AXIS, null);
            accelTaskObj = TrcTaskMgr.getInstance().createTask(instanceName, this::accelTask);
        }   //Accelerometer

        /**
         * This method enables/disables the accelerometer task that reads and caches the accelerometer data
         * periodically.
         *
         * @param enabled specifies true for enabling the accelerometer task, disabling it otherwise.
         */
        public synchronized void setEnabled(boolean enabled)
        {
            super.setEnabled(enabled);

            if (enabled)
            {
                accelTaskObj.registerTask(TrcTaskMgr.TaskType.STANDALONE_TASK, 50);
            }
            else
            {
                accelTaskObj.unregisterTask(TrcTaskMgr.TaskType.STANDALONE_TASK);
            }

            taskEnabled = enabled;
        }   //setEnabled

        /**
         * This method returns the state of the accelerometer task.
         *
         * @return true if accelerometer task is enabled, false otherwise.
         */
        public synchronized boolean isEnabled()
        {
            return taskEnabled;
        }   //isEnabled

        /**
         * This method is called periodically to read the accelerometer data.
         *
         * @param taskType specifies the type of task being run.
         * @param runMode specifies the competition mode that is running.
         */
        private synchronized void accelTask(TrcTaskMgr.TaskType taskType, TrcRobot.RunMode runMode)
        {
            timestamp = TrcUtil.getCurrentTime();

            Acceleration acceleration = imu.getAcceleration();
            Velocity velocity = imu.getVelocity();
            Position position = imu.getPosition();

            xAccel = acceleration.xAccel;
            yAccel = acceleration.yAccel;
            zAccel = acceleration.zAccel;
            xVel = velocity.xVeloc;
            yVel = velocity.yVeloc;
            zVel = velocity.zVeloc;
            xPos = position.x;
            yPos = position.y;
            zPos = position.z;
        }   //accelTask

        //
        // Implements TrcAccelerometer abstract methods.
        //

        /**
         * This method returns the raw data of the specified type for the x-axis.
         *
         * @param dataType specifies the data type.
         * @return raw data of the specified type for the x-axis.
         */
        @Override
        public synchronized SensorData<Double> getRawXData(DataType dataType)
        {
            final String funcName = "getRawXData";
            double value = 0.0;

            if (dataType == DataType.ACCELERATION)
            {
                value = xAccel;
            }
            else if (dataType == DataType.VELOCITY)
            {
                value = xVel;
            }
            else if (dataType == DataType.DISTANCE)
            {
                value = xPos;
            }
            SensorData<Double> data = new SensorData<>(TrcUtil.getCurrentTime(), value);

            if (debugEnabled)
            {
                dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
                dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                        "=(timestamp:%.3f,value:%f", data.timestamp, data.value);
            }

            return data;
        }   //getRawXData

        /**
         * This method returns the raw data of the specified type for the y-axis.
         *
         * @param dataType specifies the data type.
         * @return raw data of the specified type for the y-axis.
         */
        @Override
        public synchronized SensorData<Double> getRawYData(DataType dataType)
        {
            final String funcName = "getRawYData";
            double value = 0.0;

            if (dataType == DataType.ACCELERATION)
            {
                value = yAccel;
            }
            else if (dataType == DataType.VELOCITY)
            {
                value = yVel;
            }
            else if (dataType == DataType.DISTANCE)
            {
                value = yPos;
            }
            SensorData<Double> data = new SensorData<>(TrcUtil.getCurrentTime(), value);

            if (debugEnabled)
            {
                dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
                dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                        "=(timestamp:%.3f,value:%f", data.timestamp, data.value);
            }

            return data;
        }   //getRawYData

        /**
         * This method returns the raw data of the specified type for the z-axis.
         *
         * @param dataType specifies the data type.
         * @return raw data of the specified type for the z-axis.
         */
        @Override
        public synchronized SensorData<Double> getRawZData(DataType dataType)
        {
            final String funcName = "getRawZData";
            double value = 0.0;

            if (dataType == DataType.ACCELERATION)
            {
                value = zAccel;
            }
            else if (dataType == DataType.VELOCITY)
            {
                value = zVel;
            }
            else if (dataType == DataType.DISTANCE)
            {
                value = zPos;
            }
            SensorData<Double> data = new SensorData<>(TrcUtil.getCurrentTime(), value);

            if (debugEnabled)
            {
                dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
                dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API,
                        "=(timestamp:%.3f,value:%f", data.timestamp, data.value);
            }

            return data;
        }   //getRawZData

    }   //class Accelerometer

    public BNO055IMU imu = null;
    public TrcGyro gyro = null;
    public TrcAccelerometer accel = null;

    /**
     * Constructor: Creates an instance of the object.
     *
     * @param hardwareMap specifies the global hardware map.
     * @param instanceName specifies the instance name.
     */
    public FtcBNO055Imu(HardwareMap hardwareMap, String instanceName)
    {
        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(moduleName + "." + instanceName, tracingEnabled, traceLevel, msgLevel);
        }
        //
        // Initialize the BNO055 IMU.
        //
        imu = hardwareMap.get(BNO055IMU.class, instanceName);
        initialize();
        //
        // Create the gyro object of the IMU.
        // Note that the heading data on the z-axis is in Ordinal system with a range of -180 to 180 degrees.
        // So we need to initialize the Cartesian converter with the range values.
        //
        gyro = new Gyro(instanceName);
        //
        // Note:
        // We can convert only X (roll) and Z (yaw) axes to Cartesian.
        // For Y (pitch), when pointing upright, it will return 90-degree but pitching forward or backward will
        // yield the same decrement. So one can't tell if it is rotating forward or backward. This makes it
        // impossible to do the Cartesian conversion.
        //
        gyro.setXValueRange(-180.0, 180.0);
        gyro.setZValueRange(-180.0, 180.0);
        //
        // Create the accelerometer object of the IMU.
        //
        accel = new Accelerometer(instanceName);
    }   //FtcBNO055Imu

    /**
     * Constructor: Creates an instance of the object.
     *
     * @param instanceName specifies the instance name.
     */
    public FtcBNO055Imu(String instanceName)
    {
        this(FtcOpMode.getInstance().hardwareMap, instanceName);
    }   //FtcBNO055Imu

    /**
     * This method initializes the IMU hardware.
     */
    private void initialize()
    {
        BNO055IMU.Parameters imuParams = new BNO055IMU.Parameters();

        if (USE_QUATERNION)
        {
            imuParams.mode = BNO055IMU.SensorMode.IMU;
            imuParams.useExternalCrystal = true;
            imuParams.pitchMode = BNO055IMU.PitchMode.WINDOWS;
        }
        else
        {
            imuParams.calibrationDataFile = "BNO055IMUCalibration.json";
        }
        imuParams.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        imuParams.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        imuParams.loggingEnabled = true;
        imuParams.loggingTag = "IMU";
        imuParams.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        imu.initialize(imuParams);
    }   //initialize

}   //class FtcBNO055Imu
