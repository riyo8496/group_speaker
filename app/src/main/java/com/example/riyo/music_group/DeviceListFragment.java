package com.example.riyo.music_group;

/**
 * Created by computer on 1/12/2018.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class DeviceListFragment extends Fragment {

    private static final String TAG = "musicbroadcaster";

    private RecyclerView mRecyclerView;
    private DeviceAdapter mAdapter;
    private TextView mStatusText;

    public DeviceListFragment()
    {

    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateView() called");
        View view = inflater.inflate(R.layout.list_fragment, container, false);

        //((TextView) view.findViewById(R.id.list_status)).setText("Currently available devices");

        mRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));



        mAdapter = new DeviceAdapter(new ArrayList<SetupActivity.Endpoint>());
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }




    private class DeviceHolder extends RecyclerView.ViewHolder implements  View.OnClickListener
    {
        private SetupActivity.Endpoint mEndpoint;
        private TextView mEndpointName;


        public DeviceHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.list_item_layout, parent, false));
            mEndpointName = (TextView) itemView.findViewById(R.id.device_name);

            itemView.setOnClickListener(this);
        }


        public void bind(SetupActivity.Endpoint endpoint)
        {
            mEndpoint = endpoint;
            mEndpointName.setText(mEndpoint.getName());
        }



        @Override
        public  void onClick(View view)
        {
            ((SetupActivity) getActivity()).connectToEndpoint(mEndpoint);
        }

    }




    private class DeviceAdapter extends RecyclerView.Adapter<DeviceHolder>
    {
        List<SetupActivity.Endpoint> mEndpoints;

        public  DeviceAdapter(List<SetupActivity.Endpoint> endpoints)
        {
            mEndpoints = endpoints;
        }



        public void setEndpoints(List<SetupActivity.Endpoint> endpoints)
        {
            Log.d(TAG, "size of list " + endpoints.size());
            mEndpoints = endpoints;

        }




        @Override
        public DeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            return new DeviceHolder(inflater, parent);
        }



        @Override
        public void onBindViewHolder(DeviceHolder holder, int position) {
            holder.bind(mEndpoints.get(position));
        }



        @Override
        public int getItemCount() {
            return mEndpoints.size();
        }


    }



    public void updateUI() {
        mAdapter.notifyDataSetChanged();
    }



    public void setList(List<SetupActivity.Endpoint> endpoints)
    {
        mAdapter.setEndpoints(endpoints);

        updateUI();

    }
}


