/**
@file PureFocusSetup.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.text.DecimalFormat;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Setup dialog for monitoring line reads and configuring the PF-850.
 */
@SuppressWarnings(value = {"serial", "static-access"})  
public class PureFocusSetupDialog extends JDialog
{
    /** Reference to the MM/ImageJ GUI */
	private final Studio gui_;
    
    /** Reference to the plugin which owns this */
	private final PureFocus plugin_;
    
    /** Reference to the frame which owns this */
	private final PureFocusFrame parent_;
    
    LimitedChartPanel graphPanel_;
    private JFreeChart graph_;
    private XYSeries graphData_;
    

	/** Create dialog.  This is always instantiated when the main window is
     * created, and is shown or hidden as required.
	 * @param parent Base window
     * @param plugin PureFocus plugin
	 * @param gui MM script interface
   	 */
	public PureFocusSetupDialog(PureFocusFrame parent, PureFocus plugin, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Setup");
            
		parent_ = parent;
        plugin_ = plugin;
        gui_ = gui;
        
		initComponents();
        
        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
		updateValues(true);
	}


	/** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	private void initComponents()
	{
        int i;
        
        this.setLayout(new MigLayout("", "", ""));
        
        graphData_ = new XYSeries("a");
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(graphData_);
        graph_ = ChartFactory.createXYLineChart("", "", "", dataset);
        graphPanel_ = new LimitedChartPanel(graph_, 0.0, 1500.0, 100.0, 0.0, 4096.0, 256.0, Color.blue);
        
        // Initialise graph data/display
        graphData_.setMaximumItemCount(1500);
        for (i = 0; i < 1500; i ++)
        {
            graphData_.add((double)i, 0.0);
        }
            
        // Add to layout  
        this.add(graphPanel_, "wrap");
        
        pack();        
    }    


    public void updateValues(boolean allValues)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();

        try
        {
            for (int i = 0; i < 6; i++)
            {
                String values = core.getProperty(pf, plugin_.LINE_DATA + Integer.toString(i + 1));

                for (int j = 0; j < 250; j++ )
                {
                    if (values.length() < ((j + 1) * 3))
                    {
                        graphData_.updateByIndex((i * 250) + j, (double)0.0);
                    }
                    else
                    {
                        String value = values.substring((j * 3), (j * 3) + 3);
                        int valueInt = Integer.parseInt(value, 16);
                        graphData_.updateByIndex((i * 250) + j, (double)valueInt);
                    }
                }                    
            }
        }
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}  
    }
    
    /*
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();            
        Object source = e.getSource();
        String propertyName = e.getActionCommand();
        
        
        
    }*/












}