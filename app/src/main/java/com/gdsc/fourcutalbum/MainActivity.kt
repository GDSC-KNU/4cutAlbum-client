package com.gdsc.fourcutalbum

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.gdsc.fourcutalbum.databinding.ActivityMainBinding
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.gdsc.fourcutalbum.adapter.MainSampleAdapter
import com.gdsc.fourcutalbum.data.db.FourCutsDatabase
import com.gdsc.fourcutalbum.data.model.FourCuts
import com.gdsc.fourcutalbum.data.repository.FourCutsRepositoryImpl
import com.gdsc.fourcutalbum.viewmodel.FourCutsViewModel
import com.gdsc.fourcutalbum.viewmodel.FourCutsViewModelProviderFactory
import com.gdsc.fourcutalbum.viewmodel.MainViewModel
import com.gdsc.fourcutalbum.viewmodel.MainViewModelProviderFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    lateinit var mainViewModel: MainViewModel
    private lateinit var navController: NavController

    // Google Login
    private var isLogined = false
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onStart() {
        super.onStart()
        isLogined = getLoginState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupJetpackNavigation()
        setLogin()

        // Room db test
//        var intent : Intent = Intent(MainActivity@this, TestActivity::class.java)
//        startActivity(intent)

        val database = FourCutsDatabase.getInstance(this)
        val fourCutsRepository = FourCutsRepositoryImpl(database)

        val factory = MainViewModelProviderFactory(fourCutsRepository)
        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun setupJetpackNavigation() {
        // 내비게이션 컨트롤러 인스턴스 취득
        val host = supportFragmentManager
            .findFragmentById(R.id.fourcutalbum_nav_host) as NavHostFragment? ?: return
        navController = host.navController
        // 내비게이션 뷰를 내비게이션 컨트롤러와 연결
        binding.bottomNavigationView.setupWithNavController(navController)
    }

    private fun getLoginState() : Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let {
            Toast.makeText(this, "자동 로그인 완료!", Toast.LENGTH_SHORT).show()
            binding.btnLogin.visibility = View.GONE
            return true
        } ?: return false
    }

    private fun setLogin(){
        // ActivityResultLauncher
        setResultSignUp()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener {
            signIn()
        }
    }

    private fun setResultSignUp() {
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                // 정상적으로 결과가 받아와진다면 조건문 실행
                if (result.resultCode == Activity.RESULT_OK) {
                    val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleSignInResult(task)
                }
            }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account?.email.toString()
            val familyName = account?.familyName.toString()
            val givenName = account?.givenName.toString()
            val displayName = account?.displayName.toString()
            val photoUrl = account?.photoUrl.toString()

            Log.d("로그인한 유저의 이메일", email)
            Log.d("로그인한 유저의 성", familyName)
            Log.d("로그인한 유저의 이름", givenName)
            Log.d("로그인한 유저의 전체이름", displayName)
            Log.d("로그인한 유저의 프로필 사진의 주소", photoUrl)

            Toast.makeText(applicationContext, "$givenName 님 환영합니다!", Toast.LENGTH_SHORT).show()
            binding.btnLogin.visibility = View.GONE

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("failed", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.getSignInIntent()
        resultLauncher.launch(signInIntent)
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                // ...
            }
    }

    private fun revokeAccess() {
        mGoogleSignInClient.revokeAccess()
            .addOnCompleteListener(this) {
                // ...
            }
    }

    private fun GetCurrentUserProfile() {
        val curUser = GoogleSignIn.getLastSignedInAccount(this)
        curUser?.let {
            val email = curUser.email.toString()
            val familyName = curUser.familyName.toString()
            val givenName = curUser.givenName.toString()
            val displayName = curUser.displayName.toString()
            val photoUrl = curUser.photoUrl.toString()
            val uid = curUser.idToken.toString()

            Log.d("유저의 고유 UID", uid)
            Log.d("유저의 이메일", email)
            Log.d("유저의 성", familyName)
            Log.d("유저의 이름", givenName)
            Log.d("유저의 전체이름", displayName)
            Log.d("유저의 프로필 사진의 주소", photoUrl)

        }
    }

}