package com.liveweather.audio.extensions;

import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;
import java.io.File;
import java.util.ArrayList;

public class MIDI {

    private Sequencer midip;

    private Sequence sequence;

    private Info[] devs;

    public int devID, devDiv;

    private MidiDevice midid;

    public File[] soundbanks;

    public String[] devx;

    public String[][] devd;

    public long midiPauseProg, midiPauseProgMs, midiLoopStart, midiLoopEnd;

    public enum cycleType {
        none, whole, partial
    }

    public cycleType repeat;

    public MIDI() {
        repeat = cycleType.none;
        Info[] vdevs = MidiSystem.getMidiDeviceInfo();
        ArrayList<Info> xdevs = new ArrayList<Info>();
        ArrayList<String> src = new ArrayList<String>();
        ArrayList<String[]> dsc = new ArrayList<String[]>();
        for (Info dev : vdevs) {
            String s = dev.getName();
            try {
                MidiDevice vc = MidiSystem.getMidiDevice(dev);
                vc.getReceiver();
                vc.close();
            } catch (MidiUnavailableException e) {
                s = "$NORECEIVER";
            }
            if (s != "Real Time Sequencer" && s != "$NORECEIVER") {
                xdevs.add(dev);
                if (s == "Gervill")
                    s = "Internal";
                String dest = s;
                src.add(dest);
                String[] destc = new String[3];
                destc[0] = dev.getDescription();
                destc[1] = dev.getVendor();
                destc[2] = dev.getVersion();
                dsc.add(destc);
            }
        }
        devDiv = xdevs.size();
        Info[] arrw = new Info[xdevs.size()];
        devs = xdevs.toArray(arrw);
        String[] arrx = new String[src.size()];
        devx = src.toArray(arrx);
        String[][] arry = new String[dsc.size()][3];
        devd = dsc.toArray(arry);
        try {
            midid = MidiSystem.getMidiDevice(devs[0]);
            midid.open();
            midip = MidiSystem.getSequencer(false);
            midip.open();
            midip.getTransmitter().setReceiver(midid.getReceiver());
        } catch (Exception e) {

        }
    }

    public void changeDev(int id) {
        midiPauseProg = midip.getTickPosition();
        midiPauseProgMs = midip.getMicrosecondPosition();
        devID = devFix(id);
        boolean running = midip.isRunning();
        try {
            midip.close();
            midid.close();
            midid = MidiSystem.getMidiDevice(devs[devID]);
            midid.open();
            if (midid.getDeviceInfo().getName().equals("Gervill"))
                initSoundbank();
            midip.open();
            midip.getTransmitter().setReceiver(midid.getReceiver());
            midip.setSequence(sequence);
            changeCycleMethod();
            if (running)
                midip.start();

        } catch (Exception e) {

        }
        midip.setTickPosition(midiPauseProg);
    }

    public void setSoundbank(File[] sb) {
        soundbanks = sb;
        if (midid.getDeviceInfo().getName().equals("Gervill"))
            changeDev(devID);
    }

    private void initSoundbank() {
        Synthesizer midis = (Synthesizer) midid;
        if (soundbanks != null) {
            for (File sb : soundbanks) {
                try {
                    Soundbank sbx = MidiSystem.getSoundbank(sb);
                    midis.loadAllInstruments(sbx);
                } catch (Exception e) {

                }
            }
        }
    }

    public long getMidiProg() {
        return midip.getMicrosecondPosition();
    }

    public long getMidiTickProg() {
        return midip.getTickPosition();
    }

    public void changeMidi(File file, boolean playNow) {
        try {
            sequence = MidiSystem.getSequence(file);
            midip.setSequence(sequence);
            changeCycleMethod();
            midiPauseProg = 0;
            midiPauseProgMs = 0;
            if (playNow)
                midip.start();
        } catch (Exception e) {

        }
    }

    public void shutdown() {
        midip.close();
        midid.close();
    }

    public boolean togglePause() {
        try {
            if (isPlaying()) {
                midiPauseProg = midip.getTickPosition();
                midiPauseProgMs = midip.getMicrosecondPosition();
                midip.stop();
            } else {
                midip.start();
                midip.setTickPosition(midiPauseProg);
            }
            return true;
        } catch (Exception e) {
            if (e instanceof IllegalStateException)
                return false;
        }
        return false;
    }

    public void midiStop() {
        if (isPlaying())
            midip.stop();
        midip.setTickPosition(0);
        midiPauseProg = 0;
        midiPauseProgMs = 0;
    }

    public Boolean isPlaying() {
        return midip.isRunning();
    }

    public long getLength() {
        return midip.getMicrosecondLength();
    }

    public long getTickLength() {
        return midip.getTickLength();
    }

    public cycleType changeCycleMethod() {
        switch (repeat) {
            case none:
                midip.setLoopCount(0);
                midip.setLoopStartPoint(0);
                midip.setLoopEndPoint(-1);
                break;
            case whole:
                midip.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                midip.setLoopStartPoint(0);
                midip.setLoopEndPoint(-1);
                break;
            case partial:
                midip.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                if (midiLoopStart < midiLoopEnd) {
                    midip.setLoopStartPoint(midiLoopStart);
                    midip.setLoopEndPoint(midiLoopEnd);
                } else {
                    midip.setLoopStartPoint(0);
                    midip.setLoopEndPoint(-1);
                }
                break;
            default:
                break;
        }
        return repeat;
    }

    public void midiNavigate(long time) {
        long timeTicks = msToTicks(time);
        if (!(time != 0 && timeTicks == 0) && !((repeat == cycleType.partial && timeTicks >= midiLoopEnd)
                | time >= midip.getMicrosecondLength())) {
            if (time >= 0) {
                midip.setMicrosecondPosition(time);
                midiPauseProg = timeTicks;
                midiPauseProgMs = time;

            } else {
                midip.setMicrosecondPosition(0);
                midiPauseProg = 0;
                midiPauseProgMs = 0;
            }
        }
    }

    public void midiTickNavigate(long time) {
        if (!((repeat == cycleType.partial && time >= midiLoopEnd) | time >= midip.getTickLength())) {
            if (time >= 0) {
                midip.setTickPosition(time);
                midiPauseProg = time;
                midiPauseProgMs = midip.getMicrosecondPosition();
            } else {
                midip.setTickPosition(0);
                midiPauseProg = 0;
                midiPauseProgMs = 0;
            }
        }
    }

    private int devFix(int id) {
        if (id >= devDiv)
            return id + 1;
        else
            return id;
    }

    public long msToTicks(long ms) {
        if (ms == 0)
            return 0;
        float fps = sequence.getDivisionType();
        try {
            if (fps == Sequence.PPQ)
                return (long) (ms * midip.getTempoInBPM() * sequence.getResolution() / 60000000);
            else if (fps > Sequence.PPQ)
                return (long) (ms * fps * sequence.getResolution() / 1000000);
            else
                throw new Exception();
        } catch (Exception e) {
            return 0;
        }
    }
}