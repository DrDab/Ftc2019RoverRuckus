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

import common.AutoCommon;
import common.CmdDisplaceMineral;
import trclib.TrcEvent;
import trclib.TrcRobot;
import trclib.TrcStateMachine;
import trclib.TrcTimer;

public class CmdAutoCrater3543 implements TrcRobot.RobotCommand
{
    private static final boolean debugXPid = true;
    private static final boolean debugYPid = true;
    private static final boolean debugTurnPid = true;

    private static final String moduleName = "CmdAutoCrater3543";

    private Robot3543 robot;
    private AutoCommon.Alliance alliance;
    private double delay;
    private boolean doMineral;
    private boolean doTeamMarker;
    private boolean doTeammateMineral;

    private TrcEvent event;
    private TrcTimer timer;
    private TrcStateMachine<State> sm;
    private double targetX = 0.0;
    private double targetY = 0.0;
    private TrcRobot.RobotCommand cmdDisplaceMineral = null;

    CmdAutoCrater3543(Robot3543 robot, AutoCommon.Alliance alliance, double delay,
                      boolean startHung, boolean doMineral, boolean doTeamMarker, boolean doTeammateMineral)
    {
        robot.tracer.traceInfo(moduleName,
                "Alliance=%s,Delay=%.0f,startHung=%s,Mineral=%s,TeamMarker=%s,TeammateMineral=%s",
                alliance, delay, startHung, doMineral, doTeamMarker, doTeammateMineral);

        this.robot = robot;
        this.alliance = alliance;
        this.delay = delay;
        this.doMineral = doMineral;
        this.doTeamMarker = doTeamMarker;
        this.doTeammateMineral = doTeammateMineral;

        event = new TrcEvent(moduleName);
        timer = new TrcTimer(moduleName);
        sm = new TrcStateMachine<>(moduleName);
        sm.start(startHung? State.DO_DELAY: doMineral? State.DO_MINERAL: State.TURN_TO_WALL);
    }   //CmdAutoCrater3543

    private enum State
    {
        DO_DELAY,
        LOWER_ROBOT,
        UNHOOK_ROBOT,
        DO_MINERAL,
        DISPLACE_MINERAL,
        TURN_TO_WALL,
        DRIVE_TO_WALL,
        TURN_TO_DEPOT,
        DRIVE_TO_DEPOT,
        DROP_TEAM_MARKER,
        DRIVE_TO_CRATER,
        DRIVE_FROM_MID_WALL_TO_CRATER,
        DONE
    }   //enum State

    //
    // Implements the TrcRobot.RobotCommand interface.
    //

    @Override
    public boolean cmdPeriodic(double elapsedTime)
    {
        State state = sm.checkReadyAndGetState();
        State nextState;
        boolean traceState = true;

        if (state == null)
        {
            robot.dashboard.displayPrintf(1, "State: Disabled");
        }
        else
        {
            robot.dashboard.displayPrintf(1, "State: %s", state);

            switch (state)
            {
                case DO_DELAY:
                    //
                    // Do delay if we need to wait for partner to lower their robot first.
                    //
                    if (delay > 0.0)
                    {
                        timer.set(delay, event);
                        sm.waitForSingleEvent(event, State.LOWER_ROBOT);
                        break;
                    }
                    else
                    {
                        sm.setState(State.LOWER_ROBOT);
                    }
                    //
                    // Intentionally falling through.
                    //
                case LOWER_ROBOT:
                    //
                    // The robot started hanging on the lander, lower it to the ground.
                    //
                    robot.elevator.setPosition(RobotInfo3543.ELEVATOR_HANGING_HEIGHT, event, 0.0);
                    sm.waitForSingleEvent(event, State.UNHOOK_ROBOT);
                    break;

                case UNHOOK_ROBOT:
                    //
                    // The robot is still hooked, need to unhook first.
                    //
                    robot.tracer.traceInfo(moduleName, "Initial heading=%f", robot.driveBase.getHeading());
                    targetX = 5.0;
                    targetY = 0.0;
                    nextState = doMineral? State.DO_MINERAL: State.TURN_TO_WALL;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, nextState);
                    break;


                case DO_MINERAL:
                    //
                    // Set up CmdDisplaceMineral to use vision to displace the gold mineral.
                    //
                    robot.elevator.setPosition(RobotInfo3543.ELEVATOR_MIN_HEIGHT);
                    cmdDisplaceMineral = new CmdDisplaceMineral(robot, false);
                    sm.setState(State.DISPLACE_MINERAL);
                    //
                    // Intentionally falling through.
                    //
                case DISPLACE_MINERAL:
                    //
                    // Run CmdDisplaceMineral until it's done.
                    //
                    traceState = false;
                    if (cmdDisplaceMineral.cmdPeriodic(elapsedTime))
                    {
                        sm.setState(State.TURN_TO_WALL);
                    }
                    else
                    {
                        break;
                    }
                    //
                    // Intentionally falling through.
                    //
                case TURN_TO_WALL:
                    //
                    // We are at the starting position, let's turn towards mid-wall.
                    //
                    robot.elevator.setPosition(RobotInfo3543.ELEVATOR_MIN_HEIGHT);
                    targetX = 0.0;
                    targetY = 0.0;
                    robot.targetHeading = -90.0;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, State.DRIVE_TO_WALL);
                    break;

                case DRIVE_TO_WALL:
                    //
                    // Drive to mid-wall.
                    //
                    targetX = 0.0;
                    targetY = 54.0;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, State.TURN_TO_DEPOT);
                    break;

                case TURN_TO_DEPOT:
                    //
                    // Turn parallel to the wall and facing the depot.
                    //
                    targetX = 0.0;
                    targetY = 0.0;
                    robot.targetHeading -= 45.0;
                    nextState = doTeamMarker? State.DRIVE_TO_DEPOT: State.DRIVE_FROM_MID_WALL_TO_CRATER;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, nextState);
                    break;

                case DRIVE_TO_DEPOT:
                    //
                    // Go to the depot to drop off team marker.
                    //
                    targetX = 0.0;
                    targetY = 36.0;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, State.DROP_TEAM_MARKER);
                    break;

                case DROP_TEAM_MARKER:
                    //
                    // Release team marker by opening the deployer.
                    //
                    robot.teamMarkerDeployer.open();
                    timer.set(4.0, event);
                    sm.waitForSingleEvent(event, State.DRIVE_TO_CRATER);
                    break;

                case DRIVE_TO_CRATER:
                    //
                    // Go to the crater and park there.
                    //
                    targetX = 0.0;
                    targetY = -72.0;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, State.DONE);
                    break;

                case DRIVE_FROM_MID_WALL_TO_CRATER:
                    //
                    // Go to the crater and park there.
                    //
                    targetX = 0.0;
                    targetY = -36.0;
                    robot.pidDrive.setTarget(targetX, targetY, robot.targetHeading, false, event);
                    sm.waitForSingleEvent(event, State.DONE);
                    break;

                case DONE:
                default:
                    //
                    // We are done.
                    //
                    sm.stop();
                    break;
            }

            if (traceState)
            {
                robot.traceStateInfo(elapsedTime, state.toString(), targetX, targetY, robot.targetHeading);
            }
        }

        if (robot.pidDrive.isActive())
        {
            robot.tracer.traceInfo("Battery", "Voltage=%5.2fV (%5.2fV)",
                    robot.battery.getVoltage(), robot.battery.getLowestVoltage());
            robot.tracer.traceInfo("Raw Encoder",
                    "lf=%.0f, rf=%.0f, lr=%.0f, rr=%.0f",
                    robot.leftFrontWheel.getPosition(),
                    robot.rightFrontWheel.getPosition(),
                    robot.leftRearWheel.getPosition(),
                    robot.rightRearWheel.getPosition());

            if (debugXPid)
            {
                robot.encoderXPidCtrl.printPidInfo(robot.tracer, elapsedTime);
            }

            if (debugYPid)
            {
                robot.encoderYPidCtrl.printPidInfo(robot.tracer, elapsedTime);
            }

            if (debugTurnPid)
            {
                robot.gyroPidCtrl.printPidInfo(robot.tracer, elapsedTime);
            }
        }

        return !sm.isEnabled();
    }   //cmdPeriodic

}   //class CmdAutoCrater3543