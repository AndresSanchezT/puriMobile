package com.andresDev.puriapp.ui.adminData

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andresDev.puriapp.R
import com.andresDev.puriapp.databinding.FragmentAdminDataBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDataFragment : Fragment() {

    private var _binding: FragmentAdminDataBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentAdminDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}