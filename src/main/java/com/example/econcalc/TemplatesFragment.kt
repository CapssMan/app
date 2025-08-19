package com.example.econcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton

class TemplatesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_templates, container, false)

        val realEstateButton: MaterialButton = view.findViewById(R.id.real_estate_button)
        val startupButton: MaterialButton = view.findViewById(R.id.startup_button)
        val investmentButton: MaterialButton = view.findViewById(R.id.investment_button)

        realEstateButton.setOnClickListener {
            findNavController().navigate(R.id.action_templatesFragment_to_realEstateFragment)
        }

        startupButton.setOnClickListener {
            findNavController().navigate(R.id.action_templatesFragment_to_startupFragment)
        }

        investmentButton.setOnClickListener {
            findNavController().navigate(R.id.action_templatesFragment_to_investmentFragment)
        }

        return view
    }
}