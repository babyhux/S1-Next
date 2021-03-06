package me.ykrank.s1next.widget;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

import me.ykrank.s1next.BuildConfig;
import me.ykrank.s1next.util.FilePickerUtil;
import me.ykrank.s1next.util.L;
import me.ykrank.s1next.util.LooperUtil;
import me.ykrank.s1next.util.RxJavaUtil;
import me.ykrank.s1next.util.SQLiteUtil;

import static me.ykrank.s1next.util.FilePickerUtil.dirPickIntent;
import static me.ykrank.s1next.util.FilePickerUtil.filePickIntent;
import static me.ykrank.s1next.util.FilePickerUtil.onFilePickResult;

/**
 * Created by AdminYkrank on 2016/4/21.
 * 设置数据库进行备份的代理
 */
public class BackupDelegate {
    public static final int SUCCESS = 0;
    public static final int NO_DATA = 1;
    public static final int PERMISSION_DENY = 2;
    public static final int IO_EXCEPTION = 3;
    public static final int CANCELED = 4;
    public static final int UNKNOWN_EXCEPTION = 99;
    private static final String BACKUP_FILE_NAME = "S1Next_v" + BuildConfig.VERSION_CODE + ".bak";
    private static final int BACKUP_FILE_CODE = 11;
    private static final int RESTORE_FILE_CODE = 12;
    private Context mContext;
    private AfterBackup afterBackup;
    private AfterRestore afterRestore;

    public BackupDelegate(Context context, AfterBackup afterBackup, AfterRestore afterRestore) {
        this.mContext = context;
        this.afterBackup = afterBackup;
        this.afterRestore = afterRestore;

    }

    public void backup(Fragment fragment) {
        Intent intent = dirPickIntent(mContext);
        fragment.startActivityForResult(intent, BACKUP_FILE_CODE);
    }

    public void restore(Fragment fragment) {
        Intent intent = filePickIntent(mContext);
        fragment.startActivityForResult(intent, RESTORE_FILE_CODE);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BACKUP_FILE_CODE) {
            onFilePickResult(resultCode, data, new FilePickerUtil.OnFilePickCallback() {
                @Override
                public void success(@NonNull File file) {
                    RxJavaUtil.workWithUiResult(() -> doBackup(file), o -> afterBackup.accept(o), this::error);
                }

                @Override
                public void cancel() {
                    afterBackup.accept(CANCELED);
                }

                @Override
                public void error(Throwable e) {
                    L.e("BackupSetting:", e);
                }
            });
            return true;
        } else if (requestCode == RESTORE_FILE_CODE) {
            onFilePickResult(resultCode, data, new FilePickerUtil.OnFilePickCallback() {
                @Override
                public void success(@NonNull File file) {
                    RxJavaUtil.workWithUiResult(() -> doRestore(file), o -> afterRestore.accept(o), this::error);
                }

                @Override
                public void cancel() {
                    afterRestore.accept(CANCELED);
                }

                @Override
                public void error(Throwable e) {
                    L.e("RestoreSetting:", e);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    @WorkerThread
    @BackupResult
    private int doBackup(File destDir) {
        LooperUtil.enforceOnWorkThread();
        try {
            String dirPath = destDir.getPath();
            if (destDir.isDirectory()) {
                if (!dirPath.endsWith("/")) {
                    dirPath += "/";
                }
                File dbFile = mContext.getDatabasePath(BuildConfig.DB_NAME);
                File destFile = new File(dirPath + BACKUP_FILE_NAME);
                if (!destFile.exists()) {
                    destFile.createNewFile();
                }
                Files.copy(dbFile, destFile);
                return SUCCESS;
            } else return IO_EXCEPTION;
        } catch (IOException e) {
            L.e("BackupError:", e);
            if (e.getMessage().contains("Permission denied")) { 
                return PERMISSION_DENY;
            } else return IO_EXCEPTION;
        } catch (Exception e) {
            L.e("BackupError:", e);
            return UNKNOWN_EXCEPTION;
        }
    }

    @WorkerThread
    @BackupResult
    private int doRestore(File srcFile) {
        LooperUtil.enforceOnWorkThread();
        try {
            String filePath = srcFile.getPath();
            if (srcFile.isFile()) {
                if (SQLiteUtil.isValidSQLite(filePath)) {
                    File dbFile = mContext.getDatabasePath(BuildConfig.DB_NAME);
                    Files.copy(srcFile, dbFile);
                    return SUCCESS;
                }
            }
            return NO_DATA;
        } catch (IOException e) {
            L.e("RestoreError:", e);
            if (e.getMessage().contains("Permission denied")) {
                return PERMISSION_DENY;
            } else return IO_EXCEPTION;
        } catch (Exception e) {
            L.e("RestoreError:", e);
            return UNKNOWN_EXCEPTION;
        }
    }

    @IntDef({SUCCESS, CANCELED, NO_DATA, PERMISSION_DENY, IO_EXCEPTION, UNKNOWN_EXCEPTION})
    public @interface BackupResult {
    }

    public interface AfterBackup {
        void accept(@BackupResult Integer integer);
    }

    public interface AfterRestore {
        void accept(@BackupResult Integer integer);
    }
}
