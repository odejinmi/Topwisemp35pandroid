package com.a5starcompany.topwisemp35p.emvreader.database.table;

import android.content.Context;
import android.util.Log;

import com.a5starcompany.topwisemp35p.emvreader.database.BaseDaoImpl;
import com.topwise.cloudpos.struct.TlvList;

import java.util.List;

public class CapkDaoImpl extends BaseDaoImpl<Capk> {
    public CapkDaoImpl(Context context) {
        super(new MyDBHelper(context), Capk.class);
    }

    /**
     * select capk from database
     * @param rid    the hexstring of rid
     * @param index   index
     * @return
     */
//    public Capk findByRidIndex(String rid,byte index){
//        StringBuffer sb = new StringBuffer("select * from tb_capk where rid='")
//                .append(rid).append("' and rindex='").append(index).append("'");
//        List<Capk> capklist = rawQuery(sb.toString(), null);
//        if(capklist==null||capklist.size()==0){
//            return null;
//        }
//        return capklist.get(0);
//    }

    /**
     * select capk from database
     *
     * @param rid   the hexstring of rid
     * @param index index
     * @return
     */
    public Capk findByRidIndex(String rid, byte index) {
        String ridindex = new StringBuffer(rid).append(Integer.toHexString(index & 0xFF)).toString().toUpperCase();
        StringBuffer sb = new StringBuffer("select * from tb_capk where ridindex='").append(ridindex).append("'");
        List<Capk> capklist = rawQuery(sb.toString(), null);
        if (capklist == null || capklist.size() == 0) {
            return null;
        }
        return capklist.get(0);
    }

    /**
     * select capk from database
     *
     * @return
     */
    public List<Capk> findAllCapk() {
        StringBuffer sb = new StringBuffer("select * from tb_capk");
        List<Capk> capklist = rawQuery(sb.toString(), null);
        return capklist;
    }


    public void addCapk(String capkStr) {
        Capk capk = new Capk();
        TlvList list = new TlvList();
        Log.d("addCapk", "addCapk: " + capkStr);
        list.fromHex(capkStr);
        capk.fromTlvList(list);
        if (findByRidIndex(capk.getRid(), capk.getIndex()) != null)
            update(capk);
        else
            insert(capk);
    }

    public void deleteCapk(String ridindex) {
        //String ridindex = new StringBuffer(rid).append(Integer.toHexString(index & 0xFF)).toString().toUpperCase();
        StringBuffer sb = new StringBuffer("delete from tb_capk where ridindex=" + "'" + ridindex + "'");
        execSql(sb.toString(), null);

    }

    public void deleteAllCapk() {
        StringBuffer sb = new StringBuffer("delete from tb_capk");
        execSql(sb.toString(), null);
    }


}
