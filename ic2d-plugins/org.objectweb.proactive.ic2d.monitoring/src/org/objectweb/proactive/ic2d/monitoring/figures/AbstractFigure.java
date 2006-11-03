/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2005 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.ic2d.monitoring.figures;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public abstract class AbstractFigure extends Figure implements Runnable{
	
	// the space between top and the first child
	protected final static int topShift = 25;
	// The space between borders and children
	protected final static int shift = 10;
	
	protected Label label = new Label();
	
	/* Colors to use */
	protected Color borderColor;
	protected Color backgroundColor;
	protected Color shadowColor;
	
	protected Color highlight;
	
	protected static boolean showShadow = false; 
	
	protected boolean legend;
	
	//
	// -- CONSTRUCTORS -----------------------------------------------
	//
	protected AbstractFigure(String text){
		super();
		
		this.legend = false;
		
		// Initialisation
		this.label = new Label(text);
		initFigure();
		initColor();
		setToolTip(new ToolTipFigure(text));
	}
	
	protected AbstractFigure() {
		this.legend = true;
		initFigure();
		initColor();
	}
	
	//
	// -- PUBLIC METHODS ---------------------------------------------
	//
	
	public abstract ConnectionAnchor getAnchor();
	
	public void paintFigure(Graphics graphics){
		//System.out.println("AbstractFigure.paintFigure()");
		graphics.setAntialias(SWT.ON);
		super.paintFigure(graphics);
		paintIC2DFigure(graphics);
	}
	
	public void setTitle(String title){
		this.label.setText(title);
	}
	
	public String getTitle(){
		return this.label.getText();
	}
	
	public abstract IFigure getContentPane();
	
	/**
	 * @param color The color, or null to use the default color.
	 */
	public void setHighlight(Color color) {
		this.highlight = color;
		if(highlight != null)
			this.borderColor = color;
		else
			this.borderColor = getDefaultBorderColor();
		this.repaint();
	}
	
	/**
	 * Refreshes the graphical interface.
	 * This method is used by GUIManager
	 */
	public void refresh(){ /* DO NOTHING */}
	
	//
	// -- PROTECTED METHODS --------------------------------------------
	//
	
	protected abstract void initFigure();
	
	protected abstract void initColor();
	
	protected abstract void paintIC2DFigure(Graphics graphics);

	protected abstract Color getDefaultBorderColor();
	
	public void run(){
		//System.out.println("###run run#####===##"+ this.getTitle() );
		repaint();
	}
	
}
