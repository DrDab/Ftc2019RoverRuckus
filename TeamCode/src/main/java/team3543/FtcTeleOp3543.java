/*
 * Copyright (c) 2018 Titan Robotics Club (http://www.titanrobotics.com)
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

package team3543;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import common.TeleOpCommon;
import ftclib.FtcGamepad;
import trclib.TrcGameController;
import trclib.TrcRobot;

@TeleOp(name="TeleOp3543", group="TeleOp")
public class FtcTeleOp3543 extends TeleOpCommon implements TrcGameController.ButtonHandler
{
    private static final String moduleName = "FtcTeleOp3543";
    private Robot3543 robot;

    public FtcTeleOp3543()
    {
        super(moduleName);
    }   //FtcTeleOp3543

    //
    // Implements FtcOpMode abstract method.
    //

    @Override
    public void initRobot()
    {
        super.initRobot();
        //
        // Initializing robot objects.
        //
        robot = new Robot3543(TrcRobot.RunMode.TELEOP_MODE);
        super.setRobot(robot);
    }   //initRobot

    @Override
    public void runPeriodic(double elapsedTime)
    {
        super.runPeriodic(elapsedTime);

        double elevatorPower = operatorGamepad.getRightStickY(true);
        robot.elevator.setPower(elevatorPower);

        dashboard.displayPrintf(2, "DriveBase: x=%.2f,y=%.2f,heading=%.2f",
                robot.driveBase.getXPosition(),
                robot.driveBase.getYPosition(),
                robot.driveBase.getHeading());
        dashboard.displayPrintf(3, "Elevator: power=%.1f, pos=%.1f (%s,%s)",
                elevatorPower, robot.elevator.getPosition(),
                robot.elevator.isLowerLimitSwitchActive(), robot.elevator.isUpperLimitSwitchActive());
    }   //runPeriodic

    //
    // Implements TrcGameController.ButtonHandler interface.
    //

    @Override
    public void buttonEvent(TrcGameController gamepad, int button, boolean pressed)
    {
        dashboard.displayPrintf(
                7, "%s: %04x->%s", gamepad.toString(), button, pressed? "Pressed": "Released");
        if (gamepad == driverGamepad)
        {
            switch (button)
            {
                case FtcGamepad.GAMEPAD_A:
                    break;

                case FtcGamepad.GAMEPAD_B:
                    break;

                case FtcGamepad.GAMEPAD_X:
                    break;

                case FtcGamepad.GAMEPAD_Y:
                    break;

                case FtcGamepad.GAMEPAD_LBUMPER:
                    drivePowerScale = pressed? 0.5: 1.0;
                    break;

                case FtcGamepad.GAMEPAD_RBUMPER:
                    invertedDrive = pressed;
                    break;
            }
        }
        else if (gamepad == operatorGamepad)
        {
            switch (button)
            {
                case FtcGamepad.GAMEPAD_A:
                    break;

                case FtcGamepad.GAMEPAD_B:
                    break;

                case FtcGamepad.GAMEPAD_X:
                    break;

                case FtcGamepad.GAMEPAD_Y:
                    break;

                case FtcGamepad.GAMEPAD_LBUMPER:
                    if (pressed)
                    {
                        robot.teamMarkerDeployer.close();
                    }
                    break;

                case FtcGamepad.GAMEPAD_RBUMPER:
                    if (pressed)
                    {
                        robot.teamMarkerDeployer.open();
                    }
                    break;

                case FtcGamepad.GAMEPAD_DPAD_UP:
                    if (pressed)
                    {
                        robot.mineralSweeper.retract();
                    }
                    break;

                case FtcGamepad.GAMEPAD_DPAD_DOWN:
                    if (pressed)
                    {
                        robot.mineralSweeper.extend();
                    }
                    break;

                case FtcGamepad.GAMEPAD_DPAD_LEFT:
                    break;

                case FtcGamepad.GAMEPAD_DPAD_RIGHT:
                    break;

                case FtcGamepad.GAMEPAD_BACK:
                    if (pressed)
                    {
                        robot.elevator.zeroCalibrate();
                    }
                    break;

                case FtcGamepad.GAMEPAD_START:
                    break;
            }
        }
    }   //buttonEvent

}   //class FtcTeleOp3543
