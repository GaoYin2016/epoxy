package com.airbnb.epoxy.sample.models;

import android.content.Context;
import android.support.annotation.Nullable;
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
  public void setTitle(CharSequence text) {
    setText(text);
  }

  @ResetView
  public void clear() {

  }
}
