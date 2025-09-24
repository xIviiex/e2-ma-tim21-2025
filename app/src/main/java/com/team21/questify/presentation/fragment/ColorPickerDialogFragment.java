package com.team21.questify.presentation.fragment; // Prilagodite Vašem paketu

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team21.questify.R;
import com.team21.questify.presentation.adapter.ColorAdapter;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerDialogFragment extends DialogFragment {


    public interface ColorPickerListener {
        void onColorSelected(String hexColor);
    }

    private ColorPickerListener listener;

    public void setColorPickerListener(ColorPickerListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_color_picker, null);

        RecyclerView rvColorPicker = view.findViewById(R.id.rv_dialog_color_picker);


        List<String> colorList = new ArrayList<>();
        colorList.add("#F44336"); colorList.add("#E91E63"); colorList.add("#9C27B0");
        colorList.add("#673AB7"); colorList.add("#3F51B5"); colorList.add("#2196F3");
        colorList.add("#03A9F4"); colorList.add("#00BCD4"); colorList.add("#009688");
        colorList.add("#4CAF50"); colorList.add("#8BC34A"); colorList.add("#CDDC39");
        colorList.add("#FFEB3B"); colorList.add("#FFC107"); colorList.add("#FF9800");
        colorList.add("#FF5722"); colorList.add("#795548"); colorList.add("#9E9E9E");
        colorList.add("#607D8B");


        ColorAdapter colorAdapter = new ColorAdapter(colorList, hexColor -> {
            if (listener != null) {

                listener.onColorSelected(hexColor);
            }

            dismiss();
        });

        rvColorPicker.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvColorPicker.setAdapter(colorAdapter);


        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
                .setNegativeButton("Cancel", (dialog, id) -> {

                });
        return builder.create();
    }
}