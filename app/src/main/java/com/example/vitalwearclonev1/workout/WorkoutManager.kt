package com.example.vitalwearclonev1.workout

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Exercise(
    val name: String,
    val description: String,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null
)

data class WorkoutRoutine(
    val id: String,
    val name: String,
    val exercises: List<Exercise>
)

class WorkoutManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    fun getMilitaryRoutine(): WorkoutRoutine {
        return WorkoutRoutine(
            id = "military_basic",
            name = "Military Basic Training",
            exercises = listOf(
                Exercise("Push-ups", "Standard military push-ups", targetReps = 30),
                Exercise("Sit-ups", "Timed sit-ups", targetReps = 40),
                Exercise("Plank", "Hold a solid plank position", targetSeconds = 60),
                Exercise("Jumping Jacks", "Fast-paced jumping jacks", targetReps = 50),
                Exercise("Squats", "Bodyweight squats", targetReps = 30),
                Exercise("Run", "Steady pace run", targetSeconds = 600)
            )
        )
    }

    fun saveCustomRoutine(routine: WorkoutRoutine) {
        try {
            val routines = getCustomRoutines().toMutableList()
            routines.add(routine)
            
            val array = JSONArray()
            routines.forEach { r ->
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("name", r.name)
                val exArray = JSONArray()
                r.exercises.forEach { ex ->
                    val exObj = JSONObject()
                    exObj.put("name", ex.name)
                    exObj.put("desc", ex.description)
                    ex.targetReps?.let { exObj.put("reps", it) }
                    ex.targetSeconds?.let { exObj.put("secs", it) }
                    exArray.put(exObj)
                }
                obj.put("exercises", exArray)
                array.put(obj)
            }
            prefs.edit().putString("custom_routines", array.toString()).apply()
            timber.log.Timber.d("Custom routine saved: ${routine.name}")
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error saving custom routine")
        }
    }

    fun getCustomRoutines(): List<WorkoutRoutine> {
        val json = prefs.getString("custom_routines", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<WorkoutRoutine>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val exArray = obj.getJSONArray("exercises")
                val exercises = mutableListOf<Exercise>()
                for (j in 0 until exArray.length()) {
                    val exObj = exArray.getJSONObject(j)
                    exercises.add(Exercise(
                        name = exObj.getString("name"),
                        description = exObj.getString("desc"),
                        targetReps = if (exObj.has("reps")) exObj.getInt("reps") else null,
                        targetSeconds = if (exObj.has("secs")) exObj.getInt("secs") else null
                    ))
                }
                result.add(WorkoutRoutine(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    exercises = exercises
                ))
            }
            result
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error parsing custom routines")
            emptyList()
        }
    }

    fun deleteCustomRoutine(id: String) {
        try {
            val routines = getCustomRoutines().filter { it.id != id }
            val array = JSONArray()
            routines.forEach { r ->
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("name", r.name)
                val exArray = JSONArray()
                r.exercises.forEach { ex ->
                    val exObj = JSONObject()
                    exObj.put("name", ex.name)
                    exObj.put("desc", ex.description)
                    ex.targetReps?.let { exObj.put("reps", it) }
                    ex.targetSeconds?.let { exObj.put("secs", it) }
                    exArray.put(exObj)
                }
                obj.put("exercises", exArray)
                array.put(obj)
            }
            prefs.edit().putString("custom_routines", array.toString()).apply()
            timber.log.Timber.d("Custom routine deleted: $id")
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error deleting custom routine")
        }
    }
}
