package com.a5starcompany.topwisemp35p.emvreader.database.table;

import android.content.Context;
import android.util.Log;

import com.a5starcompany.topwisemp35p.emvreader.database.BaseDaoImpl;
import com.topwise.cloudpos.struct.TlvList;

import java.util.List;

public class AidDaoImpl extends BaseDaoImpl<Aid> {

    public AidDaoImpl(Context context) {
        super(new MyDBHelper(context), Aid.class);
    }

    /**
     * select all aid from database
     *
     * @return
     */
    public List<Aid> findAllAid() {
        StringBuffer sb = new StringBuffer("select * from tb_aid");
        List<Aid> aidlist = rawQuery(sb.toString(), null);
        return aidlist;
    }

    /**
     * select aid from database
     *
     * @param aid the hexstring of aid
     * @return
     */
    public Aid findByAid(String aid) {
        StringBuffer sb = new StringBuffer("select * from tb_aid where aid='")
                .append(aid).append("'");
        List<Aid> aidlist = rawQuery(sb.toString(), null);
        if (aidlist == null || aidlist.size() == 0) {
            return null;
        }
        return aidlist.get(0);
    }

    /**
     * select aid from database
     *
     * @param aid the hexstring of aid
     * @return
     */
    public Aid findByAidAndAsi(String aid) {
        List<Aid> mList = findAllAid();
        if (mList == null || mList.size() == 0) {
            return null;
        } else {
            for (Aid cAid : mList) {
                Log.d("findByAidAndAsi", "aid.getAid(): " + cAid.getAid());
                if (aid.startsWith(cAid.getAid())) {
                    return cAid;
                }
            }
        }
        return null;
    }

    /**
     * add one AID
     */
    public void addAid(String aidStr) {
        Aid aid = new Aid((byte) 0x00);
        TlvList list = new TlvList();
        list.fromHex(aidStr);
        aid.fromTlvList(list);
        if (findByAid(aid.getAid()) != null)
            update(aid);
        else
            insert(aid);
    }

    public void deleteAid(String rid) {
        StringBuffer sb = new StringBuffer("delete from tb_aid where aid=" + "='" + rid + "'");
        execSql(sb.toString(), null);

    }

    public void deleteAllAid() {
        StringBuffer sb = new StringBuffer("delete from tb_aid");
        execSql(sb.toString(), null);
    }


}
