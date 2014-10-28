

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

import java.applet.Applet;
import java.awt.Button;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.io.*;
import java.net.*;

public class GRT extends Applet implements ActionListener
{
	// comment
	
	Button button1;
	Button button2;
	Font f = new Font("TimesRoman", Font.BOLD, 12);
	FontMetrics fm = getFontMetrics(f);
	String tree = "", parent = "";
	int test = 0, n, currnodes = 0, MAXY = 50, p, q;
	int[] treeArray;
	int[] parentArray;
	int[] xnode;
	int[] ynode;
	int[] previousArray;
	int[] prev_parArray;
	Graphics g;

	/**
	comment
	*/
	
	public void init()
	{
		String s = getParameter("size");
		if (s == null)
		{
			n = 6;
		}
		else n = Integer.parseInt(s);
		treeArray = new int[n];
		parentArray = new int[n];
		previousArray = new int[n];
		prev_parArray = new int[n];
		xnode = new int[n];
		ynode = new int[n];

		setLayout( new BorderLayout( 0, 0 ));
		setBackground( new Color( 255, 255, 255 ));
		addNotify();

		button2 = new Button( "previous" );
		button2.setBounds( (getInsets().left + 30), (getInsets().top + 30), 100, 20 );
		add(button2);

		button1 = new Button( "next" );
		button1.setBounds( (getInsets().left + (getSize().width-130)), (getInsets().top + 30), 100, 20 );
		add(button1);
	
		button1.addActionListener(this);
		button2.addActionListener(this);

		InitialTree();

	}

	public void InitialTree()
	{
		int i, x = 1;
		for (i=0; i<n; i++)
		{
			tree = tree + x + " ";
			treeArray[i] = x;
			previousArray[i] = x;
			prev_parArray[i] = x-2;
			parentArray[i] = x-2;
			parent = parent + parentArray[i] + " ";
			x++;
		}
		vertices();
	}

	public void NextTree()
	{
		int i;
		tree = "";
		parent = "";
		for (i=(n-1); i>=0; i--)
		{
			if (treeArray[i] > 2)
			{
				p = i;
				break;
			}
		}
		q = parentArray[p];
		stop();
		for (i=0; i<(p); i++)
		{
			previousArray[i] = treeArray[i];
			prev_parArray[i] = parentArray[i];
			tree = tree + treeArray[i] + " ";
			parent = parent + parentArray[i] + " ";
		}
		for (i=(p); i<(n); i++)
		{
			previousArray[i] = treeArray[i];
			prev_parArray[i] = parentArray[i];
			treeArray[i]= treeArray[i-(p-q)];
			if ((i-p)%(p-q)==0)
			{
				parentArray[i] = parentArray[i-(p-q)];
			}
			else
			{
				parentArray[i] =  parentArray[i-(p-q)]+(p-q);
			}
			tree = tree + treeArray[i] + " ";
			parent = parent + parentArray[i] + " ";
		}
		currnodes=0;
		vertices();
		repaint();
	}
	
	public void PreviousTree()
	{
		int i;
		tree = "";
		parent = "";
		for (i=0; i<n; i++)
		{
			treeArray[i] = previousArray[i];
			parentArray[i] = prev_parArray[i];
			tree = tree + treeArray[i] + " ";
			parent = parent + parentArray[i] + " ";
		}
		currnodes=0;
		vertices();
		repaint();		
	}
	
	public void TestTree()
	{
		int i;
		for(i=(n-1); i>=1; i--)
		{
			if(treeArray[i]!=2)
			{
				test = 1;
				break;
			}
		}
	}
	
	public void vertices()
	{
		int i, a=treeArray[0], b=treeArray[0];
		int x=((getSize().width) / 2), y=110;
		addnode(x,y);
		for(i=1; i<n; i++)
		{
			a = treeArray[i];
			if(a==b)
			{
				x = x + 25;
				addnode(x, y);
			}
			else
			{
				if(a<b)
				{
					x = x + ((b-a)+1)*25;
					y = y - (b-a)*25;
					addnode(x,y);	
				}
				else
				{
					x = x - 25;
					y = y + 25;
					if(y>MAXY)
					{
						MAXY = y;
					}
					addnode(x, y);
				}
			}
			b = a;
		}
	}

	public void addnode(int x, int y)
	{
		
		xnode[currnodes] = x;
		ynode[currnodes] = y;
		currnodes++;
		
	}
	
	public void paint(Graphics g)
	{
		g.setColor(Color.black);
		g.setFont(f);
		String title = "The rooted trees with " + n + " vertices are:";
		int xstart_title = (getSize().width - fm.stringWidth(title)) / 2;
		g.drawString(title, xstart_title, 75);
		int xstart_tree = (getSize().width - fm.stringWidth(tree)) / 2;
		g.drawString(tree, xstart_tree, 90);
		for(int i=0; i<currnodes; i++)
		{
			g.fillOval(xnode[i] - 5, ynode[i] - 5, 10, 10);
		}
		for(int i=1; i<currnodes; i++)
		{
			if(treeArray[i]>treeArray[i-1])
			{
				g.drawLine(xnode[i-1], ynode[i-1], xnode[i], ynode[i]);
			}
			if(treeArray[i]<=treeArray[i-1])
			{
				g.drawLine(xnode[parentArray[i]], ynode[parentArray[i]], xnode[i], ynode[i]);
			}
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		// test comment
		/*  */
		
		Object source = e.getSource();
		if(source == button1)
		{
			TestTree();
			if (test==1)
			{
				test = 0;
				NextTree();
			}
		}
		if(source == button2)
		{
			PreviousTree();
		}
	}
}