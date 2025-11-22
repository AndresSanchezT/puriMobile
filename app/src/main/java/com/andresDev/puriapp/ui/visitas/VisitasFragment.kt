package com.andresDev.puriapp.ui.visitas

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andresDev.puriapp.R
import com.andresDev.puriapp.databinding.FragmentVisitasBinding


class VisitasFragment : Fragment() {

    private var _binding: FragmentVisitasBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVisitasBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

}