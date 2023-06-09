package org.firstinspires.ftc.teamcode.Dune.Autonomous.RegionalsStuff;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.Dune.TurretLift;
import org.firstinspires.ftc.teamcode.drive.DriveConstants;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;


@Autonomous(name = "RED_AUTO_RIGHT")
@Disabled
public class RED_AUTO_RIGHT extends LinearOpMode {

    // class members
    ElapsedTime GlobalTimer;
    double autoTimer;
    final int liftHighPosition = 850;
    final int liftMidPosition = 630;
    int heightChangeInterval;
    boolean outakeResetReady;
    boolean outakeOutReady;
    int numCycles;
    int SignalRotation;
    int slowerVelocityConstraint;
    final double outconestackX = 27;
    final double outconestackY = -9.5;
    final double outconestackRotation = 1;

    // create class instances

    //DriveBase drivebase = new DriveBase(); // hardware classes
    TurretLift turretlift = new TurretLift();
    //Inputs inputs = new Inputs();
    SleeveDetection sleeveDetection = new SleeveDetection();
    OpenCvCamera camera;
    String webcamName = "Webcam 1"; // what our webcam is called in hardware class

    enum AutoState {
        INITIAL_TURN_DRIVE,
        PRELOAD_DRIVE,
        PRELOAD_DROP,
        DRIVE_INTO_STACK,
        WAIT_AFTER_GRAB_STACK,
        DRIVE_OUT_STACK,
        WAIT_AFTER_DUMP_MID,
        PARK,
        IDLE
    }

    // We define the current state we're on
    // Default to IDLE
    AutoState currentState = AutoState.IDLE;


    // Initialize SampleMecanumDrive

    // function runs on init
    private void Setup() {
        GlobalTimer = new ElapsedTime(System.nanoTime());
        GlobalTimer.reset();
        //inputs.resetMatchTimer();
        //drivebase.motorsSetup();
        turretlift.motorsSetup();

        currentState = AutoState.INITIAL_TURN_DRIVE; // this go here?
        autoTimer = 0;
        outakeResetReady = true;
        outakeOutReady = false;
        numCycles = 0;
        heightChangeInterval = 0;
        slowerVelocityConstraint = 20;
    }
    // Define our start pose
    Pose2d startPose = new Pose2d(35, -69, Math.toRadians(-90));

    @Override
    public void runOpMode() throws InterruptedException {
        // initialize hardware
        //drivebase.Drivebase_init(hardwareMap);
        turretlift.TurretLift_init(hardwareMap);
        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap); // road drive class

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, webcamName), cameraMonitorViewId);
        sleeveDetection = new SleeveDetection();
        camera.setPipeline(sleeveDetection);

        camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener()
        {
            @Override
            public void onOpened()
            {
                camera.startStreaming(320,240, OpenCvCameraRotation.SIDEWAYS_LEFT);
            }

            @Override
            public void onError(int errorCode) {}
        });


        // functions runs on startz
        Setup();
        // Set inital pose
        drive.setPoseEstimate(startPose);

        // trajectories that aren't changing should all be here

        Trajectory InitialTurn = drive.trajectoryBuilder(startPose, true)
                .lineToLinearHeading(new Pose2d(32, -50, Math.toRadians(4)))
                //.splineTo(new Vector2d(35, -40), Math.toRadians(-90)) // spline to spline heading, first angle is target, second angle is target angle during path
                //.splineToSplineHeading(new Pose2d(35, -12, Math.toRadians(0)), Math.toRadians(-90)) // end effects shape of spline, first angle is the target heading
                .build();


        Trajectory PreloadDrive = drive.trajectoryBuilder(InitialTurn.end(), true)
                .lineToLinearHeading(new Pose2d(30, -15.5, Math.toRadians(outconestackRotation)))
                //.lineTo(new Vector2d(33, -15))
                //.splineTo(new Vector2d(35, -40), Math.toRadians(-90)) // spline to spline heading, first angle is target, second angle is target angle during path
                //.splineToSplineHeading(new Pose2d(35, -12, Math.toRadians(0)), Math.toRadians(-90)) // end effects shape of spline, first angle is the target heading
                .build();

        Trajectory IntoConeStackPreload = drive.trajectoryBuilder(PreloadDrive.end())
                .lineTo(new Vector2d(48.5,-9.2), SampleMecanumDrive.getVelocityConstraint(slowerVelocityConstraint, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                        SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
                .build();


        Trajectory IntoConeStack = drive.trajectoryBuilder(new Pose2d(outconestackX, outconestackY, Math.toRadians(outconestackRotation)))
                .lineToLinearHeading(new Pose2d(48.5, -9.2, Math.toRadians(0)), SampleMecanumDrive.getVelocityConstraint(slowerVelocityConstraint, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                        SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
                .build();

        Trajectory ParkRight = drive.trajectoryBuilder(new Pose2d(outconestackX, outconestackY, Math.toRadians(outconestackRotation)))
                .lineTo(new Vector2d(62,-9))
                .build();

        Trajectory ParkLeft = drive.trajectoryBuilder(new Pose2d(outconestackX, outconestackY, Math.toRadians(outconestackRotation)))
                .lineTo(new Vector2d(-1,-9))
                .build();

        Trajectory ParkCentre = drive.trajectoryBuilder(new Pose2d(outconestackX, outconestackY, Math.toRadians(outconestackRotation)))
                .lineTo(new Vector2d(28,-9))
                .build();

        while (!isStarted()) {
            telemetry.addData("ROTATION: ", sleeveDetection.getPosition());
            telemetry.addData("Yellow Percent:", sleeveDetection.yelPercent);
            telemetry.addData("Blue Percent:", sleeveDetection.bluPercent);
            telemetry.addData("Red Percent:", sleeveDetection.redPercent);
            telemetry.update();
            turretlift.closeClaw();
        }


        waitForStart();
        if (isStopRequested()) return;

        // open cv changes the state at the start depending on cone rotation
        // open cv vision if statements to change variable and display telemetry here
        if (sleeveDetection.getPosition() == SleeveDetection.ParkingPosition.LEFT) {
            telemetry.addLine("Rotation Left");
            SignalRotation = 1;

        } else if (sleeveDetection.getPosition() == SleeveDetection.ParkingPosition.RIGHT) {
            telemetry.addLine("Rotation Right");
            SignalRotation = 3;
        } else {
            telemetry.addLine("Rotation Centre");
            SignalRotation = 2;

        }
        // runs instantly once
        drive.followTrajectoryAsync(InitialTurn);
        autoTimer = GlobalTimer.milliseconds(); // reset timer not rly needed here
        turretlift.closeClaw();

        while (opModeIsActive() && !isStopRequested()) {
            // Read pose
            Pose2d poseEstimate = drive.getPoseEstimate(); // gets the position of the robot

            // Print pose to telemetry
            telemetry.addData("x", poseEstimate.getX());
            telemetry.addData("y", poseEstimate.getY());
            telemetry.addData("heightChangeInterval", heightChangeInterval);
            telemetry.addData("autostate", currentState);
            telemetry.addData("number of cycles:", numCycles);
            telemetry.addData ("outtakeoutReady", outakeOutReady);
            telemetry.addData ("outtakeresetReady", outakeResetReady);

            // main switch statement logic
            switch (currentState) {
                case INITIAL_TURN_DRIVE:
                    turretlift.closeClaw();
                    if (!drive.isBusy()) { //
                        currentState = AutoState.PRELOAD_DRIVE;
                        drive.followTrajectoryAsync(PreloadDrive);
                        autoTimer = GlobalTimer.milliseconds(); // reset timer
                        turretlift.closeClaw();

                    }
                    break;

                case PRELOAD_DRIVE:
                    turretlift.closeClaw();
                    outakeOutReady(-130,1,350, liftHighPosition); // get outake ready - do timer to make it later, putt hsi in a function
                    if (!drive.isBusy() && outakeOutReady) { //
                        if (GlobalTimer.milliseconds() - autoTimer > 2500){
                            turretlift.openClaw(); // preload drop
                            telemetry.addLine("PRELOAD DROP!!");
                            autoTimer = GlobalTimer.milliseconds();
                            drive.followTrajectoryAsync(IntoConeStackPreload);
                            currentState = AutoState.PRELOAD_DROP;
                            }
                        } // this timer goes off at the start of the code
                    break;

                case PRELOAD_DROP:
                    if (GlobalTimer.milliseconds() - autoTimer > 200){
                        readyOutake();  // linkage goes in
                        if (!drive.isBusy() || turretlift.intakeTouchPressed()){ // could change to ((!drive.isBusy() || turretlift.intakeTouchPressed()) || turretlift.intakeTouchPressed())
                            telemetry.addLine("drive is finished first stack intake ready");
                            if (GlobalTimer.milliseconds() - autoTimer > 0) { // changed timer to 0
                                telemetry.addLine("grab");
                                turretlift.closeClaw();
                                autoTimer = GlobalTimer.milliseconds(); // ready to grab stack
                                currentState = AutoState.WAIT_AFTER_GRAB_STACK;
                            }
                        }
                    }
                    else {
                        turretlift.liftToInternalPID(600,1); // move the lift down when it drops
                    }
                    break;

                case WAIT_AFTER_GRAB_STACK:
                    if (GlobalTimer.milliseconds() - autoTimer > 700){
                        turretlift.linkageNearlyOut();
                        turretlift.liftToInternalPID(liftMidPosition,1);
                        if (GlobalTimer.milliseconds() - autoTimer > 800){
                            turretlift.linkageIn();
                            if (GlobalTimer.milliseconds() - autoTimer > 1300){ // timer to let the linkage extend
                                if (numCycles == 0){ // legit not needed lol
                                    Trajectory OutConeStack = drive.trajectoryBuilder(poseEstimate)
                                            .lineTo(new Vector2d(outconestackX,outconestackY), SampleMecanumDrive.getVelocityConstraint(slowerVelocityConstraint, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                                                    SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
                                            .build();

                                    drive.followTrajectoryAsync(OutConeStack);
                                    currentState = AutoState.DRIVE_OUT_STACK;
                                    autoTimer = GlobalTimer.milliseconds(); // reset timer
                                }
                                else{
                                    Trajectory OutConeStack = drive.trajectoryBuilder(poseEstimate)
                                            .lineTo(new Vector2d(outconestackX,outconestackY), SampleMecanumDrive.getVelocityConstraint(slowerVelocityConstraint, DriveConstants.MAX_ANG_VEL, DriveConstants.TRACK_WIDTH),
                                                    SampleMecanumDrive.getAccelerationConstraint(DriveConstants.MAX_ACCEL))
                                            .build();

                                    drive.followTrajectoryAsync(OutConeStack);

                                    currentState = AutoState.DRIVE_OUT_STACK;
                                    autoTimer = GlobalTimer.milliseconds(); // reset timer
                                }
                            }
                            }
                        }
                    break;

                case DRIVE_OUT_STACK:
                    outakeOutReady(130,1,liftMidPosition, liftMidPosition);

                    if (!drive.isBusy() && outakeOutReady){
                        turretlift.openClaw();
                        if (numCycles == 3){
                            currentState = AutoState.PARK;
                            autoTimer = GlobalTimer.milliseconds(); // reset timer
                            numCycles += 1;
                        }
                        else{
                            heightChangeInterval += 26; // tune this changes how high it changes each time
                            numCycles += 1;
                            currentState = AutoState.WAIT_AFTER_DUMP_MID;
                            autoTimer = GlobalTimer.milliseconds(); // reset timer
                        }
                    }
                    break;

                case WAIT_AFTER_DUMP_MID:
                    turretlift.liftToInternalPID(liftMidPosition - 300, 1);
                    if (GlobalTimer.milliseconds() - autoTimer > 200){
                        turretlift.linkageIn();
                        if (GlobalTimer.milliseconds() - autoTimer > 900){
                            currentState = AutoState.DRIVE_INTO_STACK;
                            autoTimer = GlobalTimer.milliseconds(); // reset timer
                            drive.followTrajectoryAsync(IntoConeStack);
                        }
                    }
                    break;

                case DRIVE_INTO_STACK:
                        readyOutake();
                        if (!drive.isBusy() && outakeResetReady){
                            if (GlobalTimer.milliseconds() - autoTimer > 2000) {
                                telemetry.addLine("grab");
                                turretlift.closeClaw();
                                currentState = AutoState.WAIT_AFTER_GRAB_STACK;
                                autoTimer = GlobalTimer.milliseconds(); // ready to grab stack
                            }
                        }
                    break;

                case PARK:
                    if (GlobalTimer.milliseconds() - autoTimer > 200){
                        readyOutake();
                        if (SignalRotation == 1){
                            drive.followTrajectoryAsync(ParkLeft);
                            currentState = AutoState.IDLE;
                        }
                        else if (SignalRotation == 3){
                            drive.followTrajectoryAsync(ParkRight);
                            currentState = AutoState.IDLE;
                        }
                        else{
                            drive.followTrajectoryAsync(ParkCentre);
                            currentState = AutoState.IDLE; // doesn't have to drive anywhere, already in position hopefully
                        }
                    }
                    break;

                case IDLE:
                    telemetry.addLine("WWWWWWWWWWW");
                    outakeIdle();
                    break;

            }
            // Updates driving for trajectories
            drive.update();
            telemetry.update();
        }

    }

    public void readyOutake(){
        turretlift.turretSpinInternalPID(0, 1);
        if (turretlift.turretTargetReachedInteralPID()){
            turretlift.liftToInternalPID(145 - heightChangeInterval,0.5); // could be faster
            turretlift.openClawHard();
            turretlift.linkageOutHalf();
            if (turretlift.liftTargetReachedInternalPID()){
                turretlift.linkageOut();
                outakeResetReady = true; // need a false here if (turretlift.liftTargetReachedInternalPID())
            }
        }else{
            outakeResetReady = false;
            turretlift.liftToInternalPID(400,0.5);
            turretlift.readyServos();
        }
    }

    public void outakeOutReady(int turretPosition, int liftSpeed, int liftposition, int liftposition2){ // way to use timers here
        if (turretlift.liftPos() > 300){
            turretlift.turretSpinInternalPID((int)Math.round(turretlift.degreestoTicks(turretPosition)), 1); //
            if (turretlift.turretTargetReachedInteralPID()){
                turretlift.liftToInternalPID(liftposition2, 1);
                turretlift.tiltUp();
                if (currentState == AutoState.PRELOAD_DRIVE){
                    turretlift.linkageOut();
                    if (turretlift.liftPos() > liftposition2 - 100){
                        outakeOutReady = true;
                        telemetry.addLine("lift is up");
                    }
                }
                else {
                    turretlift.linkageOutHalf();
                    if (turretlift.liftPos() > liftposition2 - 100){
                        outakeOutReady = true;
                        telemetry.addLine("lift is up");
                    }
                }
            } else {
                turretlift.closeClaw(); //whats happening here is that it is
                turretlift.linkageIn(); // making sure stuff stays in when turret is turning
            }
        }
        else{
            turretlift.liftToInternalPID(liftposition, 1);
            turretlift.closeClaw();
            turretlift.linkageIn(); // might cause trouble
            turretlift.tiltUpHalf();
            outakeOutReady = false;
        }
    }
    public void outakeIdle(){
        turretlift.turretSpinInternalPID(0, 1);
        turretlift.readyServos();
        if (turretlift.liftPos() < 400){
            turretlift.liftToInternalPID(0,0.6); // could be faster
            turretlift.closeClaw();
            turretlift.linkageIn();
            if (turretlift.liftTargetReachedInternalPID()){
                outakeResetReady = true; // need a false here if (tusrretlift.liftTargetReachedInternalPID())
            }
        }else{
            outakeResetReady = false;
            turretlift.liftToInternalPID(350,0.5);
        }
    }
}


