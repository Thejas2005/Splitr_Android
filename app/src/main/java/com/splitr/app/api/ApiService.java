package com.splitr.app.api;

import com.splitr.app.models.AuthRequest;
import com.splitr.app.models.AuthResponse;
import com.splitr.app.models.Expense;
import com.splitr.app.models.ExpenseCreate;
import com.splitr.app.models.Group;
import com.splitr.app.models.GroupExpenseCreate;
import com.splitr.app.models.GroupMember;
import com.splitr.app.models.LocationExpense;
import com.splitr.app.models.MyBalances;
import com.splitr.app.models.GroupCreate;
import com.splitr.app.models.AddMemberRequest;
import com.splitr.app.models.GenericResponse;
import com.splitr.app.models.GroupExpenseResponse;
import com.splitr.app.models.FindOrCreateGroupRequest;
import com.splitr.app.models.FindOrCreateGroupResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ---- AUTH ----
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest req);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body AuthRequest req);

    @GET("api/auth/users")
    Call<List<Map<String, Object>>> getAllUsers();

    // ---- EXPENSES ----
    @POST("api/expenses/add")
    Call<GenericResponse> addPersonalExpense(@Body ExpenseCreate req);

    @POST("api/expenses/group/add")
    Call<GroupExpenseResponse> addGroupExpense(@Body GroupExpenseCreate req);

    @GET("api/expenses")
    Call<List<Expense>> getAllExpenses();

    @GET("api/expenses/user/{user_id}")
    Call<List<Expense>> getUserExpenses(@Path("user_id") int userId);

    @GET("api/expenses/locations")
    Call<List<LocationExpense>> getLocations(@Query("user_id") Integer userId);

    @GET("api/expenses/my-balances/{user_id}")
    Call<MyBalances> getMyBalances(@Path("user_id") int userId);

    @POST("api/expenses/settle")
    Call<GenericResponse> settlePayment(@Query("split_id") int splitId);

    @DELETE("api/expenses/{expense_id}")
    Call<GenericResponse> deleteExpense(@Path("expense_id") int expenseId);

    // ---- GROUPS ----
    @POST("api/groups/create")
    Call<FindOrCreateGroupResponse> createGroup(@Body GroupCreate req);

    @POST("api/groups/add-member")
    Call<GenericResponse> addMember(@Body AddMemberRequest req);

    @POST("api/groups/find-or-create")
    Call<FindOrCreateGroupResponse> findOrCreateGroup(@Body FindOrCreateGroupRequest req);

    @GET("api/groups/members/{group_id}")
    Call<List<GroupMember>> getGroupMembers(@Path("group_id") int groupId);

    @GET("api/groups/user/{user_id}")
    Call<List<Group>> getUserGroups(@Path("user_id") int userId);

    @GET("api/groups/info/{group_id}")
    Call<Group> getGroupInfo(@Path("group_id") int groupId);
}
