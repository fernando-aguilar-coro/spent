package com.app.spent.data.sync

import java.io.ByteArrayOutputStream

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
object GoogleDriveRestService {

  private const val BACKUP_FILE_NAME = "spent_backup.json"

  fun getGoogleSignInClient(context: Context): GoogleSignInClient {
    val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestEmail()
    .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))

    return GoogleSignIn.getClient(context, builder.build())
  }

  fun getSignedInAccount(context: Context): GoogleSignInAccount? {
    return GoogleSignIn.getLastSignedInAccount(context)
  }

  suspend fun signOut(context: Context): Unit = withContext(Dispatchers.IO) {
    try {
      getGoogleSignInClient(context).signOut()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
    val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
    context.applicationContext,
    listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
    )
    credential.selectedAccount = account.account ?: account.email?.let { android.accounts.Account(it, "com.google") }

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

  suspend fun getOwnBackupFileId(
    context: Context,
    account: GoogleSignInAccount
  ): Result<String?> = withContext(Dispatchers.IO) {
    try {
      val drive = getDriveService(context, account)
      val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
      val fileList: FileList = drive.files().list()
        .setQ(query)
        .setSpaces("drive")
        .setFields("files(id, name)")
        .execute()

      val fileId = fileList.files.firstOrNull()?.id
      Result.success(fileId)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  suspend fun downloadFileById(
    context: Context,
    account: GoogleSignInAccount,
    fileId: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val drive = getDriveService(context, account)
      val outputStream = ByteArrayOutputStream()
      drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
      val json = outputStream.toString("UTF-8")
      Result.success(json)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  suspend fun shareFileWithEmail(
    context: Context,
    account: GoogleSignInAccount,
    fileId: String,
    partnerEmail: String
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val drive = getDriveService(context, account)
      val permission = com.google.api.services.drive.model.Permission().apply {
        type = "user"
        role = "reader"
        emailAddress = partnerEmail.trim()
      }
      drive.permissions().create(fileId, permission)
        .setSendNotificationEmail(false)
        .execute()
      Result.success(true)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  suspend fun searchSharedBackupFiles(
    context: Context,
    account: GoogleSignInAccount
  ): Result<List<DriveBackupFileInfo>> = withContext(Dispatchers.IO) {
    try {
      val drive = getDriveService(context, account)
      val query = "(sharedWithMe = true or name contains 'spent' or name contains 'Spent' or mimeType = 'application/json') and trashed = false"
      val fileList: FileList = drive.files().list()
        .setQ(query)
        .setFields("files(id, name, modifiedTime, owners)")
        .setPageSize(20)
        .execute()

      val results = fileList.files.map { file ->
        val modTime = file.modifiedTime?.value ?: 0L
        val owner = file.owners?.firstOrNull()?.emailAddress
        DriveBackupFileInfo(
          id = file.id,
          name = file.name ?: "spent_backup.json",
          modifiedTime = modTime,
          ownerEmail = owner
        )
      }
      Result.success(results)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }
}
