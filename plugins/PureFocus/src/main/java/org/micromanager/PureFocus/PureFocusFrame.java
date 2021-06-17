/**
@file PureFocusFrame.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.util.Vector;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;

import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Top-level GUI for the PureFocus plugin.
 * 
 * This is only instantiated by the plugin if a PureFocus PF-850 device exists.
 * The GUI can therefore assume that calls to the device adapter will work,
 * barring the normal errors of invalid values or loss of comms.
 * 
 * @todo Currently this is just a placeholder to get us started.
 * 
 * @todo We need a menu or buttons to launch child dialogs.
 */
@SuppressWarnings(value = {"serial", "static-access"})    
public class PureFocusFrame extends JFrame implements ActionListener, ChangeListener, ItemListener
{
	private final Studio gui_;
	private final CMMCore core_;
	private final PureFocus plugin_;
    
    // Child dialogs
    PureFocusObjectiveSlotTableDialog objectiveSlotTableDialog_;
    PureFocusGlobalTableDialog globalTableDialog_;

    // Menu elements
    private JCheckBoxMenuItem showObjectiveSlotConfigTable_;
    private JCheckBoxMenuItem showGlobalConfigTable_;
    private JMenuItem about_;
    
	// GUI elements
	private JSpinner objectiveSelectSpinner_;
    private JTextField offsetPositionMicrons_;
    private JTextField focusPositionMicrons_; 
    private JTextField calculationA_;
    private JTextField calculationB_;
    private JTextField calculationC_;
    private JTextField calculationD_;
    private JTextField focusPidTarget_;
    private JTextField focusPidPosition_;
    private JTextField focusPidError_;
    private JTextField focusPidOutput_;
    private JTextField focusState_;
    private JTextField timeToInFocus_;
    private JCheckBox isOffsetMoving_;
    private JCheckBox isFocusDriveMoving_;
    private JCheckBox positiveLimitSwitch_;    
    private JCheckBox negativeLimitSwitch_;  
    
    // GUI handling
    final private Timer timer_;
    private Boolean errorShown_;
    
    // Set true when reading values back, to block change events
    boolean updateInProgress_;
    
    // List of objective preset names, for use by the whole GUI
    final public Vector<String> objectivePresetNames;

   
	/** Creates new form PureFocusFrame
	@param gui MM scriptInterface
	@param plugin Holds on to parent plugin
	*/
	public PureFocusFrame(Studio gui, PureFocus plugin) 
	{
		gui_ = gui;
		core_ = gui.getCMMCore();
		plugin_ = plugin;

        errorShown_ = false;
        
        // Set up list of objective names available
        objectivePresetNames = new Vector<>();
        int i = 0;
        Boolean haveName = true;
        while (haveName)
        {
            try
            {
                core_.setProperty(plugin_.DEVICE_NAME, plugin_.ARRAY_READ_INDEX, i);
                String newName = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE_PRESET_NAMES);
                objectivePresetNames.add(newName);
                i++;
            }
            catch (Exception ex)
            {
                haveName = false;
            }
        }
        
		// Set up main window
        initComponents();

        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
        objectiveSlotTableDialog_ = new PureFocusObjectiveSlotTableDialog(this, plugin_, gui_);
        objectiveSlotTableDialog_.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        objectiveSlotTableDialog_.setVisible(false);      
        objectiveSlotTableDialog_.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    showObjectiveSlotConfigTable_.setState(false);
                    objectiveSlotTableDialog_.setVisible(false);
                }
            });

        globalTableDialog_ = new PureFocusGlobalTableDialog(this, plugin_, gui_);
        globalTableDialog_.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        globalTableDialog_.setVisible(false);      
        globalTableDialog_.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    showGlobalConfigTable_.setState(false);
                    globalTableDialog_.setVisible(false);
                }
            });
        
        // Only display and update frame contents after everything else is ready
        updateInProgress_ = false;
		pack();
        updateValues(true);

        // Run timed updates of form.  This must be the last step after everything
        // else is ready.
        timer_ = new Timer(2000, this);
        timer_.setInitialDelay(2000);
        timer_.start();         
	}


    /** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents()
	{
        setTitle("Prior PureFocus PF-850");
		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		final JFrame frame = this;
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{			
				frame.dispose();
			}
		});
        
        // Set up menus
        JMenuBar menuBar;
        JMenu menu;

        menuBar = new JMenuBar();
        
        menu = new JMenu("Advanced");
        menu.setMnemonic('A');
        menuBar.add(menu);
        
        showObjectiveSlotConfigTable_ = new JCheckBoxMenuItem("Objective slot settings");
        showObjectiveSlotConfigTable_.setMnemonic('O');
        showObjectiveSlotConfigTable_.addItemListener(this);
        showObjectiveSlotConfigTable_.setSelected(false);
        menu.add(showObjectiveSlotConfigTable_);
        
        showGlobalConfigTable_ = new JCheckBoxMenuItem("Global settings");
        showGlobalConfigTable_.setMnemonic('G');
        showGlobalConfigTable_.addItemListener(this);
        showGlobalConfigTable_.setSelected(false);
        menu.add(showGlobalConfigTable_);
        
        menu = new JMenu("Help");
        menu.setMnemonic('H');
        menuBar.add(menu);
        
        about_ = new JMenuItem("About");
        about_.setMnemonic('A');
        about_.addActionListener(this);
        menu.add(about_);       

        setJMenuBar(menuBar);

        // Set up form layout
        this.setLayout(new MigLayout("", ""));

		objectiveSelectSpinner_ = new javax.swing.JSpinner();
		objectiveSelectSpinner_.setModel(new javax.swing.SpinnerNumberModel(1, 1, 6, 1));
		objectiveSelectSpinner_.setPreferredSize(new java.awt.Dimension(100, 20));
		objectiveSelectSpinner_.addChangeListener(this);
        
        offsetPositionMicrons_ = new javax.swing.JTextField();
        offsetPositionMicrons_.setPreferredSize(new java.awt.Dimension(100, 20));
        offsetPositionMicrons_.addActionListener(this);   
        offsetPositionMicrons_.setActionCommand(plugin_.OFFSET_POSITION_MICRONS);
        
        focusPositionMicrons_ = new javax.swing.JTextField();
        focusPositionMicrons_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPositionMicrons_.addActionListener(this);   
        focusPositionMicrons_.setActionCommand(plugin_.FOCUS_POSITION_MICRONS);
        
        calculationA_ = new javax.swing.JTextField();
        calculationA_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationA_.setEditable(false);       
        
        calculationB_ = new javax.swing.JTextField();
        calculationB_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationB_.setEditable(false);       
        
        calculationC_ = new javax.swing.JTextField();
        calculationC_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationC_.setEditable(false);       
        
        calculationD_ = new javax.swing.JTextField();
        calculationD_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationD_.setEditable(false);       
        
        focusPidTarget_ = new javax.swing.JTextField();
        focusPidTarget_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidTarget_.setEditable(false);               
        
        focusPidPosition_ = new javax.swing.JTextField();
        focusPidPosition_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidPosition_.setEditable(false); 
        
        focusPidError_ = new javax.swing.JTextField();
        focusPidError_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidError_.setEditable(false); 
        
        focusPidOutput_ = new javax.swing.JTextField();
        focusPidOutput_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidOutput_.setEditable(false); 
        
        focusState_ = new javax.swing.JTextField();
        focusState_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusState_.setEditable(false); 
        
        timeToInFocus_ = new javax.swing.JTextField();
        timeToInFocus_.setPreferredSize(new java.awt.Dimension(100, 20));
        timeToInFocus_.setEditable(false); 
        
        isOffsetMoving_ = new javax.swing.JCheckBox();
        isOffsetMoving_.setEnabled(false); 
        
        isFocusDriveMoving_ = new javax.swing.JCheckBox();
        isFocusDriveMoving_.setEnabled(false); 
        
        positiveLimitSwitch_ = new javax.swing.JCheckBox();
        positiveLimitSwitch_.setEnabled(false); 
        
        negativeLimitSwitch_ = new javax.swing.JCheckBox();
        negativeLimitSwitch_.setEnabled(false); 
        
        this.add(new JLabel("Objective"), "align label");
        this.add(objectiveSelectSpinner_, "wrap");
        
        this.add(new JLabel("Offset position"), "align label");
        this.add(offsetPositionMicrons_, "wrap");
        
        this.add(new JLabel("Focus position"), "align label");
        this.add(focusPositionMicrons_, "wrap");
        
        this.add(new JLabel("Calculation A"), "align label");
        this.add(calculationA_, "wrap");
        
        this.add(new JLabel("Calculation B"), "align label");
        this.add(calculationB_, "wrap");
        
        this.add(new JLabel("Calculation C"), "align label");
        this.add(calculationC_, "wrap");
        
        this.add(new JLabel("Calculation D"), "align label");
        this.add(calculationD_, "wrap");
        
        this.add(new JLabel("Focus PID target"), "align label");
        this.add(focusPidTarget_, "wrap");
        
        this.add(new JLabel("Focus PID position"), "align label");
        this.add(focusPidPosition_, "wrap");
        
        this.add(new JLabel("Focus PID error"), "align label");
        this.add(focusPidError_, "wrap");
        
        this.add(new JLabel("Focus PID output"), "align label");
        this.add(focusPidOutput_, "wrap");
        
        this.add(new JLabel("Focus state"), "align label");
        this.add(focusState_, "wrap");
        
        this.add(new JLabel("Time to in focus"), "align label");
        this.add(timeToInFocus_, "wrap");
        
        this.add(new JLabel("Is offset moving"), "align label");
        this.add(isOffsetMoving_, "wrap");
        
        this.add(new JLabel("Is focus drive moving"), "align label");
        this.add(isFocusDriveMoving_, "wrap");       
        
        this.add(new JLabel("Positive limit switch"), "align label");
        this.add(positiveLimitSwitch_, "wrap");
        
        this.add(new JLabel("Negative limit switch"), "align label");
        this.add(negativeLimitSwitch_, "wrap");      
	}
    

    @Override
    public void dispose()
    {
        // The timer will be deleted automatically when the frame is collected,
        // but in the meantime it will still be alive and still produce events
        // on the closed window (and potentially without the device existing).
        // We must stop the timer to prevent this happening.
        timer_.stop();
        
        // Tell the plugin that it has no frame attached
        plugin_.tellFrameClosed();
        
        // Close the window
        setVisible(false);
        super.dispose();
    }

    
    public void updateValues(boolean allValues)
	{
        Boolean errored = false;
        String errorMessage = "";
        
        updateInProgress_ = true;
        
        try
        {
            String val;

            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE);
            Integer objectiveSelected = Integer.parseInt(val);
            
            // Update objective setting if not being changed
            if (!objectiveSelectSpinner_.isFocusOwner())
            {
                SpinnerModel numberModel = objectiveSelectSpinner_.getModel();
                Integer formValue = (Integer)numberModel.getValue();

                if (!objectiveSelected.equals(formValue))
                {
                    // Update GUI for different values
                    numberModel.setValue(objectiveSelected);

                    if (objectiveSlotTableDialog_ != null)
                    {
                        objectiveSlotTableDialog_.updateValues(false, objectiveSelected);
                    }
                }
            }

            // Update offset setting if not being changed
            if (!offsetPositionMicrons_.isFocusOwner())
            {
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OFFSET_POSITION_MICRONS);
                offsetPositionMicrons_.setText(val);
            }

            // Update focus setting if not being changed
            if (!focusPositionMicrons_.isFocusOwner())
            {
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_POSITION_MICRONS);
                focusPositionMicrons_.setText(val);
            }

            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.CALCULATION_ABCD);
            String tokens[] = val.split(":");
            calculationA_.setText(tokens[0]);
            calculationB_.setText(tokens[1]);
            calculationC_.setText(tokens[2]);
            calculationD_.setText(tokens[3]);
            focusPidTarget_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_TARGET));
            focusPidPosition_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_POSITION));
            focusPidError_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_ERROR));
            focusPidOutput_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_OUTPUT));
            focusState_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_STATE));
            timeToInFocus_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.TIME_TO_IN_FOCUS));

            isOffsetMoving_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.IS_OFFSET_MOVING)) != 0);
            isFocusDriveMoving_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.IS_FOCUS_DRIVE_MOVING)) != 0);
            positiveLimitSwitch_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.POSITIVE_LIMIT_SWITCH)) != 0);
            negativeLimitSwitch_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.NEGATIVE_LIMIT_SWITCH)) != 0);
            
            if (allValues)
            {
                // This is set when opening the plugin or when a new configuration
                // file is loaded.  The child dialogs also need to be updated.
                objectiveSlotTableDialog_.updateValues(true, objectiveSelected);
                globalTableDialog_.updateValues();
            }
        }
        catch (Exception ex)
        {
            errored = true;
            errorMessage = ex.getMessage();
        }

        if (errored)
        {
            if (!errorMessage.startsWith("No device with label"))
            {
                // This happens if the plugin is open and the user opens the
                // hardware configuration wizard.  We still want the plugin to
                // stay open (at least unless the user removes the device from
                // the configuration, which is handled separately), but at this
                // point it doesn't have a device attached.  We must ignore
                // these errors.
                if (!errorShown_)
                {
                    // Only show an error once, otherwise we will get a popup
                    // every timer update.  Subsequent errors will not be reported.
                    // When we get a run through without an error (i.e. the fault
                    // has gone away, perhaps by reconnecting the port) then
                    // the flag is cleared and again we will report the next
                    // error.
                    gui_.logs().showError(errorMessage);
                }
            }
        }

        errorShown_ = errored;
        
        updateInProgress_ = false;
    }


	/** @return Name of currently-selected PureFocus */
    public String getPureFocus()
    {
        return plugin_.DEVICE_NAME;
    }
    
    
    @Override
    public void stateChanged(ChangeEvent e)
	{
        String pf = getPureFocus();            
        Object source = e.getSource();
        
        if (!updateInProgress_)
        {           
            try
            {
                int newValue = (Integer)objectiveSelectSpinner_.getModel().getValue();
                core_.setProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE, newValue);

                // After objective slot has changed, update settings for other dialogs
                objectiveSlotTableDialog_.updateValues(false, newValue);        
            }
            catch (Exception ex)
            {
                gui_.logs().showError(ex.getMessage());
            }        
        }
    } 
    
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = getPureFocus();         
        Object source = e.getSource();
        String propertyName = e.getActionCommand();

        if (source.getClass() == Timer.class)
        {
            updateValues(false);
        }     
        else if (!updateInProgress_)
        {
            if (source.getClass() == JTextField.class)
            {
                try
                {
                    JTextField widget = (JTextField)source;
                    core_.setProperty(pf, propertyName, Double.valueOf(widget.getText()));
                }
                catch (Exception ex)
                {
                    gui_.logs().showError(ex.getMessage());
                }       
            }
            else if (source.getClass() == JMenuItem.class)
            {
                if (source == about_)
                {
                    JOptionPane.showMessageDialog(this,
                        "Prior PureFocus PF-850 configuration plugin\n"
                            + "Written by G Bartlett\n"
                            + "(c) Prior Scientific Instruments Ltd., 2021\n"
                            + "Distributed under the BSD license following Micro-Manager licensing\n\n"
                            + "For support, please post questions on http://forum.image.sc in\n"
                            + "category \"Development\", or email inquiries@prior.com",
                        "About",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            else
            {
                // Ignore
            }
        }    
        else
        {
            // Ignore events during update
        }
    }
    
    @Override
    public void itemStateChanged(ItemEvent e)
    {   
        String pf = getPureFocus();         
        Object source = e.getSource();        
        
        if (!updateInProgress_)
        {
            if (source.getClass() == JCheckBoxMenuItem.class)
            {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem)source;
                Boolean newState = item.isSelected();

                if (source == showObjectiveSlotConfigTable_)
                {
                    // Ensure dialog starts up with correct values and objective enabled.
                    objectiveSlotTableDialog_.updateValues(false, 
                        (Integer)objectiveSelectSpinner_.getModel().getValue());     
                    objectiveSlotTableDialog_.setVisible(newState);
                }
                else if (source == showGlobalConfigTable_)
                {
                    globalTableDialog_.setVisible(newState);
                }
                else
                {
                    // Unknown
                }     
            }
            else
            {
                // Ignore
            }      
        }
    }
}
