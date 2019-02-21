package com.zyc.zcontrol.controlItem.buttonmate;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zyc.zcontrol.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ButtonMateFragment extends Fragment {


    public ButtonMateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_button_mate, container, false);
    }

}
