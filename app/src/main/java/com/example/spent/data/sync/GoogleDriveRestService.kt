package com.example.spent.data.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object GoogleDriveRestService {

    private const val BACKUP_FILE_NAME = "spent_backup.json"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))

        if (GoogleDriveConfig.WEB_CLIENT_ID.isNotBlank()) {
            builder.requestIdToken(GoogleDriveConfig.WEB_CLIENT_ID)
        }

        return GoogleSignIn.getClient(context, builder.build())
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Spent")
            .build()
    }

    suspend fun uploadBackupToDrive(
        context: Context,
        account: GoogleSignInAccount,
        jsonString: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(context, account)

            // Search for existing file
            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val fileList: FileList = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val mediaContent = ByteArrayContent("application/json", jsonString.toByteArray(Charsets.UTF_8))

            val existingFile = fileList.files.firstOrNull()
            val fileId = if (existingFile != null) {
                // Update existing backup file
                drive.files().update(existingFile.id, null, mediaContent).execute().id
            } else {
                // Create new backup file
                val metadata = File().apply {
                    name = BACKUP_FILE_NAME
                    mimeType = "application/json"
                }
                drive.files().create(metadata, mediaContent).execute().id
            }

            Result.success(fileId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun downloadBackupFromDrive(
        context: Context,
        account: GoogleSignInAccount
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(context, account)

            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val fileList: FileList = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val targetFile = fileList.files.firstOrNull()
                ?: return@withContext Result.failure(Exception("No backup file '$BACKUP_FILE_NAME' found in Google Drive"))

            val outputStream = ByteArrayOutputStream()
            drive.files().get(targetFile.id).executeMediaAndDownloadTo(outputStream)
            val json = outputStream.toString("UTF-8")

            Result.success(json)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
