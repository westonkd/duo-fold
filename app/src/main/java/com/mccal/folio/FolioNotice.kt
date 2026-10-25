package com.mccal.folio

import android.content.Context
import android.widget.Toast

internal object FolioNotice {
    fun show(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
