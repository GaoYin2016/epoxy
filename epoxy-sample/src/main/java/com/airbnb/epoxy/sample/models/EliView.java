package com.airbnb.epoxy.sample.models;

import android.content.Context;
import android.support.annotation.AnyRes;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.airbnb.epoxy.ModelProp;
import com.airbnb.epoxy.ModelView;
import com.airbnb.epoxy.R;
import com.airbnb.epoxy.ResetView;

@ModelView(defaultLayout = R.layout.model_header)
public class EliView extends AppCompatTextView {

  public EliView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @ModelProp
  public void setTitle(@Nullable @FloatRange(from = 0.0f, to = 1.0f) float text) {
  }

  @ResetView
  public void clear() {

  }
}
