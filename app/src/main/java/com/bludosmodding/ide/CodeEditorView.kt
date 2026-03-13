package com.bludosmodding.ide

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

@Composable
fun CodeEditorView(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        visualTransformation = JavaSyntaxHighlighter(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    )
}

class JavaSyntaxHighlighter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightJavaCode(text.text),
            OffsetMapping.Identity
        )
    }

    private fun highlightJavaCode(code: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        
        val keywords = Pattern.compile("\\b(public|private|protected|class|interface|enum|extends|implements|import|package|static|final|void|int|long|float|double|boolean|if|else|for|while|return|new|try|catch|throw|throws|override|volatile|transient|native|synchronized|abstract|strictfp|super|this|case|switch|break|continue|default|do|instanceof)\\b")
        val strings = Pattern.compile("\"(?:\\\\.|[^\"])*\"|'(?:\\\\.|[^'])*'")
        val comments = Pattern.compile("//.*|/\\*(?:.|[\\n\\r])*?\\*/")
        val annotations = Pattern.compile("@\\w+")

        applyStyle(builder, code, keywords, Color(0xFFCF8E6D))
        applyStyle(builder, code, annotations, Color(0xFFBBB529))
        applyStyle(builder, code, strings, Color(0xFF6A8759))
        applyStyle(builder, code, comments, Color(0xFF808080))

        return builder.toAnnotatedString()
    }

    private fun applyStyle(builder: AnnotatedString.Builder, text: String, pattern: Pattern, color: Color) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            builder.addStyle(
                style = SpanStyle(color = color),
                start = matcher.start(),
                end = matcher.end()
            )
        }
    }
}
