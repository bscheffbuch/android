package io.homeassistant.companion.android.settings.url.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.compose.composable.HAPlainButton
import io.homeassistant.companion.android.common.compose.composable.HATextField
import io.homeassistant.companion.android.common.compose.theme.HADimens
import io.homeassistant.companion.android.common.compose.theme.HATextStyle
import io.homeassistant.companion.android.common.compose.theme.LocalHAColorScheme
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExternalUrlInputView(
    url: String?,
    focusRequester: FocusRequester,
    onSaveUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var urlInput by remember(url) { mutableStateOf(url) }
    var urlError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = HADimens.SPACE4),
    ) {
        HATextField(
            value = urlInput ?: "",
            singleLine = true,
            onValueChange = {
                urlInput = it
                urlError = false
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    urlError = !performUrlUpdate(urlInput?.trim(), url, onSaveUrl)
                    if (!urlError) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                },
            ),
            placeholder = { Text(stringResource(commonR.string.input_url)) },
            isError = urlError,
            trailingIcon = if (urlError) {
                {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(commonR.string.url_invalid),
                    )
                }
            } else {
                null
            },
            supportingText = if (urlError) {
                {
                    Text(
                        text = stringResource(commonR.string.url_parse_error),
                        style = HATextStyle.BodyMedium.copy(color = LocalHAColorScheme.current.colorBorderDangerNormal),
                    )
                }
            } else {
                null
            },
            modifier = Modifier.focusRequester(focusRequester),
        )

        if (urlInput != url && urlInput?.trim()?.toHttpUrlOrNull()?.toString() != url) {
            HAPlainButton(
                text = stringResource(commonR.string.update),
                onClick = {
                    urlError = !performUrlUpdate(urlInput?.trim(), url, onSaveUrl)
                    if (!urlError) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.align(Alignment.End).padding(top = HADimens.SPACE2),
            )
        }
    }
}

/**
 * Try saving the url with the value of the input.
 * @return boolean indicating if the url was saved successfully
 */
private fun performUrlUpdate(input: String?, current: String?, onSaveUrl: (String) -> Unit): Boolean {
    return if (input != current && input?.toHttpUrlOrNull()?.toString() != current) {
        val urlValue = input?.toHttpUrlOrNull()
        val isValid = urlValue != null
        if (isValid) {
            onSaveUrl(urlValue.toString())
        }
        isValid
    } else {
        true
    }
}
