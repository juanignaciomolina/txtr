package eu.siacs.conversations.ui;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import eu.siacs.conversations.R;

/**
 * Created by Juan on 21/02/2015.
 */
public class InitialTutorialFragment extends Fragment implements View.OnClickListener {

    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static InitialTutorialFragment create(int pageNumber) {
        InitialTutorialFragment fragment = new InitialTutorialFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public InitialTutorialFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the right layout depending on which step the user is
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(selectStepLayout(mPageNumber), container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(mPageNumber == 4) {

            final Button button_new_account = (Button) getActivity().findViewById(R.id.button_new_account);
            button_new_account.setOnClickListener(this);
            final Button button_link_account = (Button) getActivity().findViewById(R.id.button_link_account);
            button_link_account.setOnClickListener(this);
        }
    }

    private int selectStepLayout(int step) {
        switch (step) {
            case 0:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    return R.layout.fragment_tutorial_step_1;
                else
                    return R.layout.fragment_tutorial_step_1;

            case 1:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    return R.layout.fragment_tutorial_step_2;
                else
                    return R.layout.fragment_tutorial_step_2_landscape;

            case 2:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    return R.layout.fragment_tutorial_step_3;
                else
                    return R.layout.fragment_tutorial_step_3_landscape;

            case 3:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    return R.layout.fragment_tutorial_step_4;
                else
                    return R.layout.fragment_tutorial_step_4_landscape;

            case 4:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    return R.layout.fragment_tutorial_step_5;
                else
                    return R.layout.fragment_tutorial_step_5_landscape;

            default: return R.layout.fragment_tutorial_step_1;
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.button_new_account:
                startActivity(new Intent(getActivity(), CreatePinActivity.class));
                break;

            case R.id.button_link_account:
                startActivity(new Intent(getActivity(), AddPinActivity.class));
                break;
        }
    }

    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }



}
