package com.mobile.smsforwarder;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.mobile.smsforwarder.model.Mail;
import com.mobile.smsforwarder.model.Number;
import com.mobile.smsforwarder.model.Relation;
import com.mobile.smsforwarder.util.ActivityRequestCode;
import com.mobile.smsforwarder.util.DatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showDataInListView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }


    public void showDataInListView() {
        ListView relationListView = findViewById(R.id.relationListView);

        List<Relation> relations = null;
        try {
            relations = getHelper().getRelationDao().queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ArrayList<String> relationNames = new ArrayList<>();
        for (Relation r : relations)
            relationNames.add(r.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, relationNames);
        relationListView.setAdapter(adapter);

        // set onClick event for every item of list
        relationListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent addRelationIntent = new Intent(MainActivity.this, RelationActivity.class);
            addRelationIntent.putExtra(ActivityRequestCode.class.getName(), ActivityRequestCode.VIEW_RELATION_ACTIVITY_CODE);
            addRelationIntent.putExtra(Relation.class.getName(), ((TextView) view).getText().toString());
            startActivityForResult(addRelationIntent, ActivityRequestCode.VIEW_RELATION_ACTIVITY_CODE);            });

        // set onLongClick event for every item of list
        relationListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showPopup(((TextView) view).getText().toString());
            return true;
        });
    }


    public void showPopup(String relationName) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_relation);
        TextView popupTextView = dialog.findViewById(R.id.titleTextView);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button deleteButton = dialog.findViewById(R.id.deleteButton);

        popupTextView.setText("Relation " + relationName);
        cancelButton.setOnClickListener(v -> {
            Log.i("MainActivity", "Cancel button on popup_relation was clicked");
            dialog.dismiss();
        });

        deleteButton.setOnClickListener(v -> {

            try {
                Relation relation = getHelper().getRelationDao().queryBuilder().where().eq("name", relationName).query().get(0);

                DeleteBuilder<Number, Long> deleteNumberBuilder = getHelper().getNumberDao().deleteBuilder();
                deleteNumberBuilder.where().eq("relation_id", relation.getId());
                deleteNumberBuilder.delete();

                DeleteBuilder<Mail, Long> deleteMailBuilder = getHelper().getMailDao().deleteBuilder();
                deleteMailBuilder.where().eq("relation_id", relation.getId());
                deleteMailBuilder.delete();

                DeleteBuilder<Relation, Long> deleteRelationBuilder = getHelper().getRelationDao().deleteBuilder();
                deleteRelationBuilder.where().eq("name", relationName);
                deleteRelationBuilder.delete();


            } catch (SQLException e) {
                e.printStackTrace();
            }


            Log.i("MainActivity", "deletedRelation=[" + relationName + "]");
            Toast.makeText(getBaseContext(), "Relation [" + relationName + "]" + " was deleted.", Toast.LENGTH_LONG).show();

            dialog.dismiss();
            showDataInListView();
        });
        dialog.show();

    }

    public void onClick_addButton(View v) {
        Intent addRelationIntent = new Intent(MainActivity.this, RelationActivity.class);
        addRelationIntent.putExtra(ActivityRequestCode.class.getName(), ActivityRequestCode.ADD_RELATION_ACTIVITY_CODE);
        startActivityForResult(addRelationIntent, ActivityRequestCode.ADD_RELATION_ACTIVITY_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivityRequestCode.ADD_RELATION_ACTIVITY_CODE) {
            showDataInListView();
        }
    }

}
