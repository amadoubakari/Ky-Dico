package com.flys.dico.dao.db;

import android.util.Log;

import com.flys.dico.R;
import com.flys.dico.architecture.custom.DApplicationContext;
import com.flys.dico.dao.entities.User;
import com.flys.generictools.dao.daoImpl.GenericDaoImpl;
import com.flys.generictools.dao.db.DatabaseHelper;
import com.j256.ormlite.dao.Dao;

import org.androidannotations.annotations.EBean;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class UserDaoImpl extends GenericDaoImpl<User, Long> implements UserDao {

    DatabaseHelper<User, Long> databaseHelper;

    @Override
    public Dao<User, Long> getDao() {
        List<Class<?>> entityClasses = new ArrayList<>();
        entityClasses.add(User.class);
        databaseHelper = new DatabaseHelper(DApplicationContext.getContext(), R.raw.ormlite_config);
        try {
            return (Dao<User, Long>) databaseHelper.getDao(getEntityClassManaged());
        } catch (SQLException e) {
            Log.e(getClass().getSimpleName(), "getting Dao Exception!", e);
        }

        return null;
    }

    @Override
    public void flush() {
        if(databaseHelper.isOpen()){
            databaseHelper.close();
        }
    }

    @Override
    public Class<User> getEntityClassManaged() {
        return User.class;
    }
}
