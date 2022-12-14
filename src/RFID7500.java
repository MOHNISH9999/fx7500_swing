
import com.mot.rfid.api3.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RFID7500 extends JFrame {
    private JTextField textField1;
    private JButton connectButton;
    private JButton disconnectButton;

    DefaultListModel<String> listy = new DefaultListModel<>();
    DefaultListModel<String> clearlist = new DefaultListModel<>();
    int i = 0;
    LocalDateTime then;
    private JList<String> list1;
    private JPanel mainpanel;
    private JButton startButton;
    private JLabel tagreadlabel;
    private JLabel readerinfo2;
    private JLabel readtagnolabel;
    private JButton stopbutton;

    SwingWorker<Void,String> worker,worker2;

    int thsleep;

    boolean keepruning=true;


    //RFidActive rd;

    RFIDReader myReader = null;
    private boolean accessComplete = false;
    private boolean inventoryComplete = false;

    private Lock accessEventLock = new ReentrantLock();
    private Condition accessEventCondVar = accessEventLock.newCondition();

    private Lock inventoryStopEventLock = new ReentrantLock();
    private Condition inventoryStopCondVar = inventoryStopEventLock.newCondition();

    public static Hashtable<String,Long> tagStore = null;

    public static final String API_SUCCESS = "Function Succeeded";
    public static final String PARAM_ERROR = "Parameter Error";
    final String APP_NAME = "J_RFIDSample3";

    public boolean isConnected;
    public String hostName = "";
    public int port = 5084;

    String[] memoryBank = new String[] { "Reserved", "EPC", "TID", "USER" };

    public boolean isAccessSequenceRunning = false;
    String[] tagState = new String[] { "New", "Gone", "Back", "None" };

    // To display tag read count
    public long uniqueTags = 0;
    public long totalTags = 0;

    private EventsHandler eventsHandler = new EventsHandler();

    // Antennas

    public Antennas antennas;

    // Access Filter
    public AccessFilter accessFilter = null;
    public boolean isAccessFilterSet = false;

    // Post Filter
    public PostFilter postFilter = null;
    public boolean isPostFilterSet = false;

    // Antenna Info
    public AntennaInfo antennaInfo;

    // Pre Filter
    public PreFilters preFilters = null;

    public PreFilters.PreFilter preFilter1 = null;
    public PreFilters.PreFilter preFilter2 = null;

    public String preFilterTagPattern1 = null;
    public String preFilterTagPattern2 = null;

    public boolean isPreFilterSet1 = false;
    public boolean isPreFilterSet2 = false;
    public int preFilterActionIndex1 = 0;
    public int preFilterActionIndex2 = 0;

    public TriggerInfo triggerInfo = null;

    public int readerTypeIndex = 1;

    // Access Params
    TagAccess tagAccess = null;
    TagAccess.ReadAccessParams readAccessParams;
    TagAccess.WriteAccessParams writeAccessParams;
    TagAccess.LockAccessParams lockAccessParams;
    TagAccess.KillAccessParams killAccessParams;

    // Access filter

    BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
    public int rowId = 0;

    TagData[] myTags = null;


    public boolean connectToReader(String readerHostName, int readerPort)
    {


        boolean retVal = false;
        hostName = readerHostName;
        port = readerPort;
        myReader.setHostName(hostName);
        myReader.setPort(port);

        try {
            myReader.connect();

            myReader.Events.setInventoryStartEvent(true);
            myReader.Events.setInventoryStopEvent(true);
            myReader.Events.setAccessStartEvent(true);
            myReader.Events.setAccessStopEvent(true);
            myReader.Events.setAntennaEvent(true);
            myReader.Events.setGPIEvent(true);
            myReader.Events.setBufferFullEvent(true);
            myReader.Events.setBufferFullWarningEvent(true);
            myReader.Events.setReaderDisconnectEvent(true);
            myReader.Events.setReaderExceptionEvent(true);
            myReader.Events.setTagReadEvent(true);
            myReader.Events.setAttachTagDataWithReadEvent(false);

            TagStorageSettings tagStorageSettings = myReader.Config.getTagStorageSettings();
            tagStorageSettings.discardTagsOnInventoryStop(true);
            myReader.Config.setTagStorageSettings(tagStorageSettings);

            myReader.Events.addEventsListener(eventsHandler);

            retVal = true;
            isConnected = true;
            postInfoMessage("Connected to " + hostName);
            postStatusNotification(API_SUCCESS, null);
            myReader.Config.setTraceLevel(TRACE_LEVEL.TRACE_LEVEL_ERROR);

            //Createmenu();

        } catch (InvalidUsageException ex)
        {
            postStatusNotification(PARAM_ERROR, ex.getVendorMessage());
        } catch (OperationFailureException ex) {
            postStatusNotification(ex.getStatusDescription(),
                    ex.getVendorMessage());
        }


        return retVal;

    }

    private STATE_UNAWARE_ACTION getStateUnawareAction(Integer action) throws InvalidUsageException {

        switch (action) {
            case 0:
                return STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_SELECT_NOT_UNSELECT;

            case 1:
                return STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_SELECT;

            case 2:
                return STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_NOT_UNSELECT;

            case 3:
                return STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_UNSELECT;

            case 4:
                return STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_UNSELECT_NOT_SELECT;

            case 5:
                return STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_NOT_SELECT;
            default:
                throw new InvalidUsageException("InvalidUsageException", "Valid range of StateUnawareaAction [0,5]");


        }

    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return data;
    }


    private void AddPreFilter() throws NumberFormatException, InvalidUsageException, IOException, OperationFailureException {

        String tagMask;
        Integer memoryBank;
        Integer action;
        Long byteCount;
        String temp;


        System.out.println("----Command Menu----");
        System.out.println("Enter AntennaID");
        preFilter1.setAntennaID(Short.valueOf(inputReader.readLine()));


        System.out.println("Enter Memorybank ");
        System.out.println(" 1 for EPC  ");
        System.out.println(" 2 for TID  ");
        System.out.println(" 3 for USER  ");
        preFilter1.setMemoryBank(MEMORY_BANK.GetMemoryBankValue(Integer.valueOf(inputReader.readLine())));

        System.out.println(" Enter Bit OffSet ");
        preFilter1.setBitOffset(Integer.valueOf(inputReader.readLine()));


        System.out.println(" Enter TagPattern");
        preFilter1.setTagPattern(hexStringToByteArray(inputReader.readLine()));

        System.out.println(" Enter TagPattern's Bit count ");
        preFilter1.setTagPatternBitCount(Integer.valueOf(inputReader.readLine()));

        preFilter1.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_UNAWARE);

        System.out.println("Enter stateUnawareAction");
        System.out.println("0 for STATE_UNAWARE_ACTION_SELECT_NOT_UNSELECT ");
        System.out.println("1 for STATE_UNAWARE_ACTION_SELECT ");
        System.out.println("2 for STATE_UNAWARE_ACTION_NOT_UNSELECT ");
        System.out.println("3 for STATE_UNAWARE_ACTION_UNSELECT ");
        System.out.println("4 for STATE_UNAWARE_ACTION_UNSELECT_NOT_SELECT ");
        System.out.println("5 for STATE_UNAWARE_ACTION_NOT_SELECT ");

        preFilter1.StateUnawareAction.setStateUnawareAction(getStateUnawareAction(Integer.valueOf(inputReader.readLine())));

        myReader.Actions.PreFilters.deleteAll();

        myReader.Actions.PreFilters.add(preFilter1);
        System.out.println("Add PreFilter Successfully");


    }

    private void RemovePrefilter() throws InvalidUsageException, OperationFailureException {

        myReader.Actions.PreFilters.delete(preFilter1);
        System.out.println("Remove PreFilter Successfully");

    }


    public RFIDReader getMyReader() {
        return myReader;
    }

    void
    updateTags(Boolean isAccess)
    {
        TagDataArray oTagDataArray = myReader.Actions.getReadTagsEx(1000);
        myTags = oTagDataArray.getTags();

        if (myTags != null)
        {
            if(!isAccess)
            {
                for (int index = 0; index < oTagDataArray.getLength(); index++)
                {
                    TagData tag = myTags[index];
                    String key = tag.getTagID();
                    // if (!tagStore.containsKey(key))
                    // {
                    //	tagStore.put(key,totalTags);
                    postInfoMessage("ReadTag "+key);
                    worker=new SwingWorker<Void, String>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            if(isCancelled()){
                                return null;
                            }
                            publish(key);
                            return null;
                        }

                        @Override
                        protected void process(List<String> chunks) {
                            for(String line:chunks){
                                listy.addElement(chunks.toString());
                                list1.setModel(listy);
                            }
                            super.process(chunks);
                        }
                    };
                    worker.execute();
                    //listy.addElement("Tag Read: "+key);



                    readtagnolabel.setText("Tags Read: "+totalTags);




                    //uniqueTags++;
                    // }
                    totalTags++;
                }

            }
            else
            {
                for (int index = 0; index < myTags.length; index++)
                {
                    TagData tag = myTags[index];
                    if(tag.getMemoryBankData() != null)
                        postInfoMessage("TagID "+tag.getTagID()+tag.getMemoryBank().toString()+"  "+tag.getMemoryBankData());
                    else
                        postInfoMessage("TagID "+tag.getTagID()+"Access Status:  "+tag.getOpStatus().toString());

                }
            }




        }

    }



    private void DisplayCapability()
    {

//        System.out.println("Reader Capabilities\n\n");
//        System.out.println("FirwareVersion="+myReader.ReaderCapabilities.getFirwareVersion());
//        System.out.println("ModelName= "+myReader.ReaderCapabilities.getModelName());
//        System.out.println("NumAntennaSupported= "+myReader.ReaderCapabilities.getNumAntennaSupported());
//        System.out.println("NumGPIPorts= "+myReader.ReaderCapabilities.getNumGPIPorts());
//        System.out.println("NumGPOPorts= "+myReader.ReaderCapabilities.getNumGPOPorts());
//        System.out.println("IsUTCClockSupported= "+myReader.ReaderCapabilities.isUTCClockSupported());
//        System.out.println("IsBlockEraseSupported= "+myReader.ReaderCapabilities.isBlockEraseSupported());
//        System.out.println("IsBlockWriteSupported= "+myReader.ReaderCapabilities.isBlockWriteSupported());
//        System.out.println("IsTagInventoryStateAwareSingulationSupported= "+myReader.ReaderCapabilities.isTagInventoryStateAwareSingulationSupported());
//        System.out.println("MaxNumOperationsInAccessSequence= "+myReader.ReaderCapabilities.getMaxNumOperationsInAccessSequence());
//        System.out.println("MaxNumPreFilters= "+myReader.ReaderCapabilities.getMaxNumPreFilters());
//        System.out.println("CommunicationStandard= "+myReader.ReaderCapabilities.getCommunicationStandard());
//        System.out.println("CountryCode= "+myReader.ReaderCapabilities.getCountryCode());
//        System.out.println("IsHoppingEnabled= "+myReader.ReaderCapabilities.isHoppingEnabled());

        //readerinfo2.setFont(new Font("Calibri", Font.BOLD, 20));

        readerinfo2.setText(

                "<html>" +
                        "Reader Info:" +
                        "<br/>"+
                        "<br/>"+
                        "FirwareVersion="+myReader.ReaderCapabilities.getFirwareVersion()+
                        "<br/>"+
                        "ModelName= "+myReader.ReaderCapabilities.getModelName()+
                        "<br/>"+
                        "NumAntennaSupported= "+myReader.ReaderCapabilities.getNumAntennaSupported()+
                        "<br/>"+
                        "NumGPIPorts= "+myReader.ReaderCapabilities.getNumGPIPorts()+
                        "<br/>"+
                        "NumGPOPorts= "+myReader.ReaderCapabilities.getNumGPOPorts()+
                        "<br/>"+
                        "IsUTCClockSupported= "+myReader.ReaderCapabilities.isUTCClockSupported()+
                        "<br/>"+
                        "IsBlockEraseSupported= "+myReader.ReaderCapabilities.isBlockEraseSupported()+
                        "<br/>"+
                        "IsBlockWriteSupported= "+myReader.ReaderCapabilities.isBlockWriteSupported()+
                        "<br/>"+
                        "IsTagInventoryStateAwareSingulationSupported= "+myReader.ReaderCapabilities.isTagInventoryStateAwareSingulationSupported()+
                        "<br/>"+
                        "MaxNumOperationsInAccessSequence= "+myReader.ReaderCapabilities.getMaxNumOperationsInAccessSequence()+
                        "<br/>"+
                        "CommunicationStandard= "+myReader.ReaderCapabilities.getCommunicationStandard()+
                        "<br/>"+
                        "CountryCode= "+myReader.ReaderCapabilities.getCountryCode()+
                        "<br/>"+
                        "IsHoppingEnabled= "+myReader.ReaderCapabilities.isHoppingEnabled()+
                        "<br/>"+
                        //"Tags Read: "+list1.getModel().getSize()+
                        "</html>"
//                "Reader Info: \n"+
//                "\n\nFirwareVersion="+myReader.ReaderCapabilities.getFirwareVersion()+
//                "\nModelName= "+myReader.ReaderCapabilities.getModelName()+
//                "\nNumAntennaSupported= "+myReader.ReaderCapabilities.getNumAntennaSupported()+
//                "\nNumGPIPorts= "+myReader.ReaderCapabilities.getNumGPIPorts()+
//                "\nNumGPOPorts= "+myReader.ReaderCapabilities.getNumGPOPorts()+
//                "\nIsUTCClockSupported= "+myReader.ReaderCapabilities.isUTCClockSupported()+
//                "\nIsBlockEraseSupported= "+myReader.ReaderCapabilities.isBlockEraseSupported()+
//                "\nIsBlockWriteSupported= "+myReader.ReaderCapabilities.isBlockWriteSupported()+
//                "\nIsTagInventoryStateAwareSingulationSupported= "+myReader.ReaderCapabilities.isTagInventoryStateAwareSingulationSupported()+
//                "\nMaxNumOperationsInAccessSequence= "+myReader.ReaderCapabilities.getMaxNumOperationsInAccessSequence()+
//                "\nCommunicationStandard= "+myReader.ReaderCapabilities.getCommunicationStandard()+
//                "\nCountryCode= "+myReader.ReaderCapabilities.getCountryCode()+
//                "\nIsHoppingEnabled= "+myReader.ReaderCapabilities.isHoppingEnabled()
        );



    }



    void postStatusNotification(String statusMsg, String vendorMsg)
    {
        System.out.println("Status: "+statusMsg+" Vendor Message: "+vendorMsg);
    }

    static void postInfoMessage(String msg)
    {
        System.out.println(msg);
    }

    private void SimpleInventory() throws InterruptedException,InvalidUsageException,OperationFailureException
    {

        tagStore.clear();


        myReader.Actions.Inventory.perform();

        System.out.println("simple inventory started");

        System.out.println("Press Enter to stop inventory");

        try
        {
            inputReader.readLine();
        }
        catch(IOException ibex)
        {
            System.out.println("IO Exception.Stopping inventory");
        }
        finally
        {
            myReader.Actions.Inventory.stop();

        }

        try
        {
            inventoryStopEventLock.lock();
            if(!inventoryComplete)
            {
                inventoryStopCondVar.await();
                inventoryComplete = false;
            }

        }
        finally
        {
            inventoryStopEventLock.unlock();
        }
    }

    private void PeriodicInventory() throws InterruptedException,InvalidUsageException,OperationFailureException
    {
        System.out.println("Periodic inventory started");

        try
        {
            tagStore.clear();

            this.triggerInfo = new TriggerInfo();

            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_PERIODIC);
            //triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_DURATION);

            SYSTEMTIME startTime = new SYSTEMTIME();
            Calendar  calendar = Calendar.getInstance();

            startTime.Year=(short)calendar.get(Calendar.YEAR);
            startTime.Month=(short)calendar.get(Calendar.MONTH);
            startTime.Day=(short)calendar.get(Calendar.DAY_OF_MONTH);
            startTime.Hour=(short)calendar.get(Calendar.HOUR_OF_DAY);
            startTime.Minute=(short)calendar.get(Calendar.MINUTE);
            startTime.Second=(short)calendar.get(Calendar.SECOND);
            startTime.Second += 3;
            startTime.Milliseconds=0;

            triggerInfo.StartTrigger.Periodic.setPeriod(2000);
            triggerInfo.StartTrigger.Periodic.StartTime = startTime;
            triggerInfo.StopTrigger.setDurationMilliSeconds(1000);
            triggerInfo.setTagReportTrigger(1);



            myReader.Actions.Inventory.perform(null,triggerInfo,null);


            //Thread.sleep(1000);
            //Thread.sleep(1000000);
            Thread.sleep(Long.MAX_VALUE);

            //Thread.sleep(thsleep);


            myReader.Actions.Inventory.stop();

            this.triggerInfo = new TriggerInfo();
            this.triggerInfo.setTagReportTrigger(1);

        }
        finally
        {
            myReader.Actions.Inventory.stop();
        }


    }




    public class EventsHandler implements RfidEventsListener
    {
        public EventsHandler()
        {

        }

        public void eventReadNotify(RfidReadEvents rre) {

            updateTags(false);
        }





        public void eventStatusNotify(RfidStatusEvents rse)
        {
            postInfoMessage(rse.StatusEventData.getStatusEventType().toString());

            STATUS_EVENT_TYPE statusType = rse.StatusEventData.getStatusEventType();
            if (statusType == STATUS_EVENT_TYPE.ACCESS_STOP_EVENT)
            {
                try
                {
                    accessEventLock.lock();
                    accessComplete = true;
                    accessEventCondVar.signalAll();
                }
                finally
                {
                    accessEventLock.unlock();

                }

            }
            else if(statusType == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT)
            {
                try
                {
                    inventoryStopEventLock.lock();
                    inventoryComplete = true;
                    inventoryStopCondVar.signalAll();

                }
                finally
                {
                    inventoryStopEventLock.unlock();
                }

            }
            else if(statusType == STATUS_EVENT_TYPE.BUFFER_FULL_WARNING_EVENT || statusType == STATUS_EVENT_TYPE.BUFFER_FULL_EVENT)
            {
                postInfoMessage(statusType.toString());
            }

        }
    }





    public RFID7500() {


        myReader = new RFIDReader();

        // Hash table to hold the tag data
        tagStore = new Hashtable();
        isAccessSequenceRunning = false;

        // Create the Access Filter
        accessFilter = new AccessFilter();
        accessFilter.setAccessFilterMatchPattern(FILTER_MATCH_PATTERN.A);
        accessFilter.TagPatternA = null;
        accessFilter.TagPatternB = null;

        // create the post filter
        postFilter = new PostFilter();

        // Create Antenna Info
        antennaInfo = new AntennaInfo();

        // Create Pre-Filter
        preFilters = new PreFilters();

        preFilter1 = preFilters.new PreFilter();
        preFilter2 = preFilters.new PreFilter();

        antennas = myReader.Config.Antennas;

        triggerInfo = new TriggerInfo();

        triggerInfo.StartTrigger
                .setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
        triggerInfo.StopTrigger
                .setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

        triggerInfo.TagEventReportInfo
                .setReportNewTagEvent(TAG_EVENT_REPORT_TRIGGER.MODERATED);
        triggerInfo.TagEventReportInfo
                .setNewTagEventModeratedTimeoutMilliseconds((short) 500);

        triggerInfo.TagEventReportInfo
                .setReportTagInvisibleEvent(TAG_EVENT_REPORT_TRIGGER.MODERATED);
        triggerInfo.TagEventReportInfo
                .setTagInvisibleEventModeratedTimeoutMilliseconds((short) 500);

        triggerInfo.TagEventReportInfo
                .setReportTagBackToVisibilityEvent(TAG_EVENT_REPORT_TRIGGER.MODERATED);
        triggerInfo.TagEventReportInfo
                .setTagBackToVisibilityModeratedTimeoutMilliseconds((short) 500);

        triggerInfo.setTagReportTrigger(1);

        // Access Params

        tagAccess = new TagAccess();
        readAccessParams  = tagAccess.new ReadAccessParams();
        writeAccessParams = tagAccess.new WriteAccessParams();
        lockAccessParams  = tagAccess.new LockAccessParams();
        killAccessParams  = tagAccess.new KillAccessParams();


        // On Device, connect automatically to the reader
        //connectToReader("169.254.10.1", 5084);


        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String abc=textField1.getText();
                connectToReader(abc, 5084);
                //System.out.println("Antenna info: "+antennas.getLength());
                JOptionPane.showMessageDialog(connectButton, textField1.getText() + " Connected");
                DisplayCapability();
                //connectButton.setEnabled(false);

                //rd.activatethread();









                //updateTags(false);


            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    totalTags=0;

                    readerinfo2.setText("Reader Info:");
                    postInfoMessage("DisConnected to " + hostName);
                    clearlist.addElement("");
                    listy.removeAllElements();
                    list1.setModel(listy);
                    readtagnolabel.setText("Tags Read: ");
                    myReader.disconnect();
                } catch (InvalidUsageException ex) {
                    throw new RuntimeException(ex);
                } catch (OperationFailureException ex) {
                    throw new RuntimeException(ex);
                }

//                list1.setModel(listy);

                JOptionPane.showMessageDialog(disconnectButton, textField1.getText() + " DisConnected");
                connectButton.setEnabled(true);
                tagreadlabel.setText("Choose time duration to read tags in secs..");
            }
        });
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {


//                while (keepruning){
//                    new Thread() {
//                        @Override
//                        public void run() {
//                            try {
//                                SimpleInventory();
//                            } catch (InterruptedException ex) {
//                                throw new RuntimeException(ex);
//                            } catch (InvalidUsageException ex) {
//                                throw new RuntimeException(ex);
//                            } catch (OperationFailureException ex) {
//                                throw new RuntimeException(ex);
//                            }
//                            super.run();
//                        }
//                    }.start();
//                }

                worker2=new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        if(isCancelled()){
                            return null;
                        }
                        PeriodicInventory();
                        //SimpleInventory();
                        return null;
                    }
                };
                worker2.execute();

//                try {
//
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
//                } catch (InvalidUsageException ex) {
//                    throw new RuntimeException(ex);
//                } catch (OperationFailureException ex) {
//                    throw new RuntimeException(ex);
//                }




                //list1.setModel(listy);





                //JOptionPane.showMessageDialog(startButton, "Reading Completed");



            }
        });


//        timedur.addChangeListener(new ChangeListener() {
//            @Override
//            public void stateChanged(ChangeEvent e) {
//
//                thsleep=(int)timedur.getValue()*1000;
//                tagreadlabel.setText("Read tags for "+thsleep/1000+" secs?");
//            }
//        });
        stopbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //keepruning=false;
                worker.cancel(true);
                worker2.cancel(true);
            }
        });
        startButton.setBorder(new LineBorder(Color.BLACK));
        stopbutton.setBorder(new LineBorder(Color.BLACK));
        connectButton.setBorder(new LineBorder(Color.BLACK));
        disconnectButton.setBorder(new LineBorder(Color.BLACK));
        readerinfo2.setBorder(new LineBorder(Color.ORANGE));
    }


    public static void main(String[] args) {



        RFID7500 d = new RFID7500();
        d.setContentPane(d.mainpanel);
        d.setTitle("FX7500");
        d.setSize(750, 600);
        d.setVisible(true);


    }


//    class RFidActive {
//        public void activatethread() {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    String hostname = textField1.getText();
//                    // On Device, connect automatically to the reader
//                    //connectToReader(hostname, 5084);
//                    myReader=new RFIDReader(hostname,5084,5000);
//                    if (myReader != null) {
//                        System.out.printf("reader not null", String.valueOf(myReader));
//                    }
//                    try {
//                        //connectToReader(hostname,5084);
//
//
//
//
//
//                        Antennas.AntennaRfConfig antennaRfConfig = myReader.Config.Antennas.getAntennaRfConfig(1);
//                        antennaRfConfig.setrfModeTableIndex(0);
//                        antennaRfConfig.setTari(0);
//                        antennaRfConfig.setTransmitPowerIndex(270);
//                        //set the configuration
//                        myReader.Config.Antennas.setAntennaRfConfig(1,antennaRfConfig);
//
//
//
//
//
//                        //reader.Events.addEventsListener(eventHandler);
//
//                        //GPIO (General purpose input output)
//
////                        gpiPortState = myReader.Config.GPI.getPortState(1);
////                        gpoPortState = myReader.Config.GPO.getPortState(1);
////                        numGPIPorts = myReader.ReaderCapabilities.getNumGPIPorts();
////                        numGPOPorts = myReader.ReaderCapabilities.getNumGPOPorts();
////                        myReader.Config.GPI.enablePort(1, true);
////                        portEnabled = myReader.Config.GPI.isPortEnabled(1);
////                        myReader.Config.GPO.setPortState(1, GPO_PORT_STATE.TRUE);
//
//                        //GPIO (General purpose input output)
//
////                        Log.d("\nReader id: ", reader.ReaderCapabilities.ReaderID.getID());
////                        Log.d("\nmodel name: ", reader.ReaderCapabilities.getModelName());
////                        Log.d("\ncommunication standard: ", reader.ReaderCapabilities.getCommunicationStandard().toString());
////                        Log.d("\ncountry code: ", String.valueOf(reader.ReaderCapabilities.getCountryCode()));
////                        Log.d("\nfirmware version: ", reader.ReaderCapabilities.getFirwareVersion());
////                        Log.d("\nRSSI filter: ", String.valueOf(reader.ReaderCapabilities.isRSSIFilterSupported()));
//
//                        ConfigureReader();
//                        myReader.Actions.Inventory.perform();
//
//                    }
//                    catch (InvalidUsageException | OperationFailureException e) {
//                        e.printStackTrace();
//                    }
//
//
//
//
//
//
//
//
//
//
//                }
//                public EventHandler eventHandler;
//
//                private void ConfigureReader() {
//                    if (myReader.isConnected()) {
//                        TriggerInfo triggerInfo = new TriggerInfo();
//                        triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
//                        triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
//                        // receive events from reader
//                        if (eventHandler == null)
//                            eventHandler = new EventHandler();
//                        myReader.Events.addEventsListener(eventHandler);
//                        myReader.Events.setInventoryStartEvent(true);
//                        myReader.Events.setInventoryStopEvent(true);
//                        // tag event with tag data
//                        myReader.Events.setTagReadEvent(true);
//                        // application will collect tag using getReadTags API
//                        myReader.Events.setAttachTagDataWithReadEvent(false);
//                        myReader.Events.setBufferFullEvent(true);
//                        myReader.Events.setBufferFullWarningEvent(true);
//                        myReader.Events.setReaderDisconnectEvent(true);
//
//                    }
//                }
//                class EventHandler implements RfidEventsListener {
//                    // Read Event Notification
//                    boolean conn=false;
//                    public void eventReadNotify(RfidReadEvents e) {
//
//                        new Runnable() {
//                            @Override
//                            public void run() {
//
//
//
//
//
//                                TagData[] tag=myReader.Actions.getReadTags(100);
////                                            textView=findViewById(R.id.TagText);
////                                            listView=findViewById(R.id.listview);
////                                            textview2=findViewById(R.id.viewdev);
////                                            textview2.setText("Reader id: "+reader.ReaderCapabilities.ReaderID.getID()+
////                                                            "\nmodel name: "+reader.ReaderCapabilities.getModelName()+
////                                                            "\ncommunication standard: "+reader.ReaderCapabilities.getCommunicationStandard().toString()+
////                                                            "\ncountry code: "+String.valueOf(reader.ReaderCapabilities.getCountryCode())+
////                                                            "\nfirmware version: "+reader.ReaderCapabilities.getFirwareVersion()+
////                                                            "\nRSSI filter: "+String.valueOf(reader.ReaderCapabilities.isRSSIFilterSupported()+
////                                                            "\nGPIPortState: "+gpiPortState+
////                                                            "\nGPOPortState: "+gpoPortState+
////                                                            "\nnumGPIPorts: "+numGPIPorts+
////                                                            "\nnumGPOPorts: "+numGPOPorts+
////                                                            "\nportEnabled(GPI): "+portEnabled+
////                                                            "\ndemo: "+htoip
////                                                    )
////                                            );
//                                if(tag!=null){
//                                    //short[] m=reader.Config.Antennas.;
////                                                if(stopper!=false){
////                                                    try {
////                                                        reader.Events.removeEventsListener(eventHandler);
////
////                                                        //reader.disconnect();
////                                                        //reader = null;
////                                                        Toast.makeText(getApplicationContext(), "Disconnecting reader", Toast.LENGTH_LONG).show();
////
////
////                                                    } catch (InvalidUsageException e) {
////                                                        e.printStackTrace();
////                                                    } catch (OperationFailureException e) {
////                                                        e.printStackTrace();
////                                                    }
////                                                }
//                                    i=i+tag.length;
//                                    if(then==null){
//                                        then= LocalDateTime.now();
//                                    }
//                                    Duration diff=Duration.between(then, LocalDateTime.now());
//                                    if(diff.getSeconds()>1){
//
//                                        //textView.setText("tags read: "+i);
//                                        //+"\nTime: "+LocalDateTime.now().toString());
//                                        for(int i=0;i<tag.length;i++){
////                                            tglist.add(String.valueOf(tag[i].getTagID()));
////                                            ArrayAdapter<String> adapter=
////                                                    new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1,tglist);
////                                            listView.setAdapter(adapter);
////                                            Log.i("tags:",String.valueOf(tag[i].getTagID()));
//                                            listy.addElement(tag[i].getTagID());
//                                            list1.setModel(listy);
//
//                                        }
//                                        then=LocalDateTime.now();
//                                        i=0;
//
//                                    }
//                                }
//                                tag=null;
////
//                            }
//                        };
//
//
//                    }
//                    @Override
//                    public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
//                        //Log.d("Status Notification: ",String.valueOf(rfidStatusEvents.StatusEventData.getStatusEventType()));
//                        if(rfidStatusEvents.StatusEventData.getStatusEventType()== DISCONNECTION_EVENT){
//                            //textView.setText("Disconnection event");
//                        }
//
//                    }
//                }
//            });
//        }
//    }
}
