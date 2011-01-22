/*
 * EnterpriseWizard
 *
 * Copyright (C) 20010 EnterpriseWizard, Inc. All Rights Reserved.
 *
 * \$Id$
 * Created by Konstantin Sobolev (kos@enterprisetwizard.com) on 21.01.11
 * Last modification \$Date$
 */

package com.kos.ktodo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;
import com.kos.ktodo.R;
import com.kos.ktodo.TodoItemBackgroundDrawable;

/**
 * Text view for one line in a widget. Adds priority color mark.
 * UNFINISHED.. CAN'T BE ACCESSED FROM REMOTE VIEW, ANDROID LIMITATION
 *
 * @author <a href="mailto:kos@supportwizard.com" title="">Konstantin Sobolev</a>
 * @version $Revision$
 */
public class WidgetTextView extends TextView {
	private final int[] prioToColor;
	private TodoItemBackgroundDrawable bg;

	public WidgetTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WidgetTextView);

		prioToColor = new int[]{
				ta.getColor(R.styleable.WidgetTextView_prio0Color, context.getResources().getColor(R.color.prio_0)),
				ta.getColor(R.styleable.WidgetTextView_prio1Color, context.getResources().getColor(R.color.prio_1)),
				ta.getColor(R.styleable.WidgetTextView_prio2Color, context.getResources().getColor(R.color.prio_2)),
				ta.getColor(R.styleable.WidgetTextView_prio3Color, context.getResources().getColor(R.color.prio_3)),
				ta.getColor(R.styleable.WidgetTextView_prio4Color, context.getResources().getColor(R.color.prio_4)),
				ta.getColor(R.styleable.WidgetTextView_prio5Color, context.getResources().getColor(R.color.prio_5)),
		};

		final int prioStripeWidth = (int) ta.getDimension(R.styleable.WidgetTextView_prioStripeWidth, 1);
		bg = new TodoItemBackgroundDrawable(Color.TRANSPARENT, Color.TRANSPARENT, prioStripeWidth);
		bg.setPercent(0);
		setBackgroundDrawable(bg);
		ta.recycle();
	}

	public void setPrio(final int prio) {
		bg.setPrioColor(prioToColor[prio]);
	}
}
