package com.tobusan.selfidrone.view;

import com.tobusan.selfidrone.drone.BebopDrone;
import com.tobusan.selfidrone.drone.Beeper;

public class WideShot{
    private final static String CLASS_NAME = WideShot.class.getSimpleName();

    private Thread WideThread = null;

    private BebopDrone bebopDrone = null;

    private Beeper beeper = null;

    public void resume(final BebopDrone bebopDrone, final Beeper beeper) {
        this.bebopDrone = bebopDrone;
        this.beeper = beeper;

        WideThread = new CascadingThread();
        WideThread.start();
    }

    public void pause() {
        WideThread.interrupt();
        try {
            WideThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class CascadingThread extends Thread {

        private void stop_shot() {
            bebopDrone.setPitch((byte)0);
            bebopDrone.setFlag((byte)0);
            bebopDrone.setGaz((byte)0);
        }

        private void start_shot() {
            bebopDrone.setPitch((byte)-10);
            bebopDrone.setFlag((byte)1);
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                bebopDrone.setPitch((byte)0);
                bebopDrone.setFlag((byte)0);
            }
            bebopDrone.setPitch((byte)0);
            bebopDrone.setFlag((byte)0);

            bebopDrone.setGaz((byte)30);
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                bebopDrone.setGaz((byte)0);
            }
            bebopDrone.setGaz((byte)0);

            beeper.play();
            bebopDrone.takePicture();

            bebopDrone.setGaz((byte)-30);
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                bebopDrone.setGaz((byte)0);
            }
            bebopDrone.setGaz((byte)0);

            bebopDrone.setPitch((byte)10);
            bebopDrone.setFlag((byte)1);
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                bebopDrone.setPitch((byte)0);
                bebopDrone.setFlag((byte)0);
            }
            bebopDrone.setPitch((byte)0);
            bebopDrone.setFlag((byte)0);
        }

        @Override
        public void interrupt() {
            stop_shot();
        }
        @Override
        public void run() {
            start_shot();
        }
    }
}